package com.example.chat.chat

import com.example.chat.chat.dto.InboxEvent
import com.example.chat.chat.dto.MessageResponse
import com.example.chat.chat.dto.ReadReceipt
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Best-effort live pushes for chat events. Delivery is never guaranteed (the DB is the truth) — a
 * push to a destination nobody is subscribed to simply goes nowhere, which is exactly how the
 * read-enforcement works: full messages go to the per-conversation destination only open views
 * receive, while the inbox carries metadata to everyone online.
 */
@Component
class ChatNotifier(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    /** Full message → an open view of the thread (`/user/queue/conv/{id}`). */
    fun message(userId: String, conversationId: String, message: MessageResponse) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/conv/$conversationId", message)
    }

    /** Metadata for the conversation list (`/user/queue/inbox`). */
    fun inbox(userId: String, event: InboxEvent) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/inbox", event)
    }

    /** Tell [authorUserId] that their messages in a thread were read (`/user/queue/read`). */
    fun readReceipt(authorUserId: String, receipt: ReadReceipt) {
        messagingTemplate.convertAndSendToUser(authorUserId, "/queue/read", receipt)
    }
}
