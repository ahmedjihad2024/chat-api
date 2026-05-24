package com.example.chat.auth.mail

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.mailer"], havingValue = "smtp")
class SmtpMailer(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val from: String,
) : Mailer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(email: String, code: String) {
        val message = SimpleMailMessage().apply {
            this.from = this@SmtpMailer.from
            setTo(email)
            subject = "Your verification code"
            text = "Your verification code is: $code\n\nThis code expires in $VERIFICATION_CODE_TTL_MINUTES minutes."
        }
        try {
            mailSender.send(message)
        } catch (ex: Exception) {
            log.error("Failed to send verification code to {}", email, ex)
        }
    }
}
