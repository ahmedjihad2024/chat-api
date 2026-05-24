package com.example.chat.auth.mail

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.mailer"], havingValue = "log", matchIfMissing = true)
class LogMailer : Mailer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(email: String, code: String) {
        log.info("[Mailer] Verification code for {} = {}", email, code)
    }
}
