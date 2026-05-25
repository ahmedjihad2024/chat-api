package com.example.chat.auth.sms

import org.springframework.scheduling.annotation.Async

interface SmsSender {
    // Runs on the `smsExecutor` thread pool so SMS-provider latency never blocks
    // the request thread. Callers cannot rely on send completion for control flow.
    @Async("smsExecutor")
    fun sendVerificationCode(phone: String, code: String)
}
