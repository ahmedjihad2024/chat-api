package com.example.chat.chat

import com.example.chat.chat.dto.ConversationReadStatus
import com.example.chat.chat.dto.ConversationResponse
import com.example.chat.chat.dto.InboxEvent
import com.example.chat.chat.dto.MessageResponse
import com.example.chat.chat.dto.PresenceEvent
import com.example.chat.chat.dto.ReadReceipt
import com.example.chat.chat.dto.SentMessage
import com.example.chat.chat.dto.SideReadStatus
import com.example.chat.chat.entities.Conversation
import com.example.chat.chat.entities.Message
import com.example.chat.chat.mapper.toResponse
import com.example.chat.chat.repository.ConversationRepository
import com.example.chat.chat.repository.MessageRepository
import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ApiException
import com.example.chat.user.mapper.toResponse
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ChatService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val activeConversations: ActiveConversationRegistry,
    private val presence: PresenceRegistry,
    private val chatNotifier: ChatNotifier,
) {

    private companion object {
        const val PREVIEW_MAX = 120
    }

    /**
     * Get-or-create the 1:1 thread between [currentUserId] and [otherUserId] (open chat — the only
     * requirement is that the other user exists). The thread is found-or-inserted atomically on the
     * unique [com.example.chat.chat.entities.Conversation.pairKey], so concurrent opens can never create a duplicate.
     */
    @Transactional
    fun openConversation(currentUserId: String, otherUserId: String): ApiResponse<ConversationResponse> {
        val me = ObjectId(currentUserId)
        val other = ObjectId(otherUserId)
        if (me == other) throw ApiException.BadRequest("error.chat.cannot_chat_self")
        val otherUser = userRepository.findById(other)
            .orElseThrow { ApiException.NotFound("error.user.not_found") }

        val conversation = conversationRepository.findOrCreate(me, other)
        val online = presence.isOnline(otherUserId)
        return ApiResponse.ok(conversation.toResponse(currentUserId, otherUser.toResponse(), online))
    }

    /** My conversations, paged, newest-active first (sort comes from the caller's Pageable). */
    @Transactional(readOnly = true)
    fun listConversations(currentUserId: String, pageable: Pageable): ApiResponse<List<ConversationResponse>> {
        val me = ObjectId(currentUserId)
        val page = conversationRepository.findByParticipantIds(me, pageable)

        val otherIds = page.content.map { it.otherParticipant(me) }
        val usersById = userRepository.findAllById(otherIds).associateBy { it.id }
        val responses = page.content.mapNotNull { convo ->
            usersById[convo.otherParticipant(me)]?.let { other ->
                val online = presence.isOnline(other.id.toHexString())
                convo.toResponse(currentUserId, other.toResponse(), online)
            }
        }
        return ApiResponse.paged(PageImpl(responses, pageable, page.totalElements))
    }

    /**
     * A user just crossed the online/offline edge: push a [PresenceEvent] to the inbox of every
     * partner they have a conversation with, so only people who actually chat with them are told.
     * Best-effort and per-instance (mirrors the rest of the live layer).
     */
    fun notifyPresence(userId: String, online: Boolean) {
        val me = ObjectId(userId)
        val event = PresenceEvent(userId, online, Instant.now())
        conversationRepository.findByParticipantIds(me)
            .forEach { convo -> chatNotifier.presence(convo.otherParticipant(me).toHexString(), event) }
    }

    /**
     * Message history for a thread, paged (newest-first via the Pageable). This is the offline
     * safety net: it returns everything in the DB, including messages received while the caller's
     * socket was closed. Fetching history also marks the thread read for the caller (so a separate
     * mark-read call isn't required). Only a participant may read a thread.
     */
    @Transactional
    fun history(currentUserId: String, conversationId: String, pageable: Pageable): ApiResponse<List<MessageResponse>> {
        val me = ObjectId(currentUserId)
        val convo = requireParticipant(me, ObjectId(conversationId))
        val page = messageRepository.findByConversationId(convo.id, pageable)
        markReadAndNotify(me, convo)
        return ApiResponse.paged(page.map { it.toResponse() })
    }

    /**
     * Mark the thread read for the caller: reset their unread counter to 0 and flip the `read`
     * flag on the messages the other user sent. Kept as a manual fallback — reading history and
     * viewing the thread live both mark read automatically. Only a participant may do this.
     */
    @Transactional
    fun markRead(currentUserId: String, conversationId: String): ApiResponse<Unit> {
        val me = ObjectId(currentUserId)
        val convo = requireParticipant(me, ObjectId(conversationId))
        markReadAndNotify(me, convo)
        return ApiResponse.ok(Unit)
    }

    /** Per-side read state of a thread (each participant's unread count). Only a participant may read it. */
    @Transactional(readOnly = true)
    fun readStatus(currentUserId: String, conversationId: String): ApiResponse<ConversationReadStatus> {
        val convo = requireParticipant(ObjectId(currentUserId), ObjectId(conversationId))
        val sides = convo.participantIds.map { id ->
            val hex = id.toHexString()
            SideReadStatus(userId = hex, unread = convo.unread[hex] ?: 0)
        }
        return ApiResponse.ok(ConversationReadStatus(convo.id.toHexString(), sides))
    }

    /**
     * Reset [reader]'s unread counter and flip their incoming messages to read; if anything was
     * newly read, push a [ReadReceipt] to the other participant so their UI updates live.
     */
    private fun markReadAndNotify(reader: ObjectId, conversation: Conversation) {
        conversationRepository.resetUnread(conversation.id, reader)
        val newlyRead = messageRepository.markIncomingRead(conversation.id, reader)
        if (newlyRead > 0) {
            val author = conversation.otherParticipant(reader)
            chatNotifier.readReceipt(
                author.toHexString(),
                ReadReceipt(conversation.id.toHexString(), reader.toHexString(), Instant.now()),
            )
        }
    }

    /**
     * Persist a message, then update the thread (preview, `lastMessageAt`, and the recipient's
     * unread `$inc`) atomically. Persistence is non-negotiable and happens here; the live push is
     * the caller's best-effort step. Returns the persisted message plus the recipient to push to.
     */
    @Transactional
    fun sendMessage(senderId: String, recipientId: String, text: String): SentMessage {
        val body = text.trim()
        if (body.isEmpty()) throw ApiException.BadRequest("error.chat.empty_message")
        val sender = ObjectId(senderId)
        val recipient = ObjectId(recipientId)
        if (sender == recipient) throw ApiException.BadRequest("error.chat.cannot_chat_self")
        if (!userRepository.existsById(recipient)) throw ApiException.NotFound("error.user.not_found")

        val conversation = conversationRepository.findOrCreate(sender, recipient)
        val convoIdHex = conversation.id.toHexString()
        // If the recipient already has this thread open, the message is read on arrival: persist it
        // read and skip their unread bump.
        val recipientViewing = activeConversations.isViewing(recipientId, convoIdHex)
        val message = messageRepository.save(
            Message(conversationId = conversation.id, senderId = sender, text = body, read = recipientViewing),
        )

        val preview = body.take(PREVIEW_MAX)
        conversationRepository.applySentMessage(
            conversation.id,
            recipient,
            preview,
            message.createdAt,
            incrementUnread = !recipientViewing,
            previewMessageRead = recipientViewing,
        )

        val response = message.toResponse()
        val inbox = InboxEvent(
            convoIdHex,
            response.senderId,
            preview,
            response.createdAt,
            response.id,
            recipientViewing
        )
        return SentMessage(response, inbox, recipient, recipientWasViewing = recipientViewing)
    }

    private fun requireParticipant(userId: ObjectId, conversationId: ObjectId): Conversation {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { ApiException.NotFound("error.chat.conversation_not_found") }
        if (userId !in conversation.participantIds) {
            // Hide existence from non-participants rather than leaking a 403.
            throw ApiException.NotFound("error.chat.conversation_not_found")
        }
        return conversation
    }

    /** The single other participant of a 1:1 thread, relative to [me]. */
    private fun Conversation.otherParticipant(me: ObjectId): ObjectId =
        participantIds.first { it != me }
}
