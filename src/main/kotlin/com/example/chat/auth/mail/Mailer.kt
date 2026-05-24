package com.example.chat.auth.mail

import org.springframework.scheduling.annotation.Async

interface Mailer {
    // Runs on the `mailExecutor` thread pool so SMTP latency never blocks the
    // request thread. Callers cannot rely on send completion for control flow.
    @Async("mailExecutor")
    fun sendVerificationCode(email: String, code: String)
}
