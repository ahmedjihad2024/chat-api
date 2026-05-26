package com.example.chat.chat.dto

import org.bson.types.ObjectId

/**
 * The result of persisting a message: the full [message] (for open chat views), the [inbox]
 * metadata (for conversation lists), who to deliver to, and whether the recipient already had the
 * thread open — in which case it was delivered already-read and the sender gets a receipt.
 */
data class SentMessage(
    val message: MessageResponse,
    val inbox: InboxEvent,
    val recipientId: ObjectId,
    val recipientWasViewing: Boolean,
)
