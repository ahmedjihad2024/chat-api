package com.example.chat.chat.repository

import org.bson.types.ObjectId

/**
 * Message data-access beyond derived queries. Implemented in [MessageRepositoryImpl] and mixed
 * into [MessageRepository].
 */
interface MessageRepositoryCustom {

    /**
     * Flip `read` on the thread's messages NOT sent by [reader] (the ones they received).
     * Returns how many were newly marked read, so a read receipt is pushed only when it changed.
     */
    fun markIncomingRead(conversationId: ObjectId, reader: ObjectId): Long
}
