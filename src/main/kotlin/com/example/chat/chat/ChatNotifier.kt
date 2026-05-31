package com.example.chat.chat

import com.example.chat.chat.dto.InboxEvent
import com.example.chat.chat.dto.MessageResponse
import com.example.chat.chat.dto.PresenceEvent
import com.example.chat.chat.dto.ReadReceipt
import com.example.chat.chat.live.RedisLivePublisher
import org.springframework.stereotype.Component

/**
 * Best-effort live pushes for chat events. Delivery is never guaranteed (the DB is the truth) — a
 * push to a destination nobody is subscribed to simply goes nowhere, which is exactly how the
 * read-enforcement works: full messages go to the per-conversation destination only open views
 * receive, while the inbox carries metadata to everyone online.
 *
 * Pushes go out through Redis pub/sub ([RedisLivePublisher]) rather than straight to the local broker,
 * so they reach the recipient on whichever instance holds their session — making the app safe to run as
 * multiple replicas. Each push is delivered to local clients by [com.example.chat.chat.live.RedisLiveSubscriber].
 */
@Component
class ChatNotifier(
    private val livePublisher: RedisLivePublisher,
) {
    /** Full message → an open view of the thread (`/user/queue/conv/{id}`). */
    fun message(userId: String, conversationId: String, message: MessageResponse) {
        livePublisher.publish(userId, "/queue/conv/$conversationId", message)
    }

    /** Metadata for the conversation list (`/user/queue/inbox`). */
    fun inbox(userId: String, event: InboxEvent) {
        livePublisher.publish(userId, "/queue/inbox", event)
    }

    /** Tell [authorUserId] that their messages in a thread were read (`/user/queue/read`). */
    fun readReceipt(authorUserId: String, receipt: ReadReceipt) {
        livePublisher.publish(authorUserId, "/queue/read", receipt)
    }

    /** Tell [recipientUserId] that a chat partner crossed the online/offline edge (`/user/queue/inbox`). */
    fun presence(recipientUserId: String, event: PresenceEvent) {
        livePublisher.publish(recipientUserId, "/queue/inbox", event)
    }
}
