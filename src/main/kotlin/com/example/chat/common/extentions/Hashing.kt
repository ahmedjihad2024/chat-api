package com.example.chat.common.extentions

import java.security.MessageDigest
import java.util.Base64

// SHA-256 of the receiver string, Base64-encoded. Used to store one-way hashes
// of refresh tokens and verification codes so a DB leak doesn't reveal the
// plaintext value.
fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return Base64.getEncoder().encodeToString(digest)
}
