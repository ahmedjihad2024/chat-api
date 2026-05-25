package com.example.chat.auth.sms

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.sms"], havingValue = "log", matchIfMissing = true)
class LogSmsSender : SmsSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(phone: String, code: String) {
        log.info("[SMS] Verification code for {} = {}", phone, code)
    }
}
