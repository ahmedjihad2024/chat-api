package com.example.chat.auth.sms

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.sms"], havingValue = "twilio")
class TwilioSmsSender(
    @Value("\${twilio.account-sid}") private val accountSid: String,
    @Value("\${twilio.auth-token}") private val authToken: String,
    @Value("\${twilio.from-number}") private val fromNumber: String,
) : SmsSender {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        Twilio.init(accountSid, authToken)
    }

    override fun sendVerificationCode(phone: String, code: String) {
        try {
            Message.creator(
                PhoneNumber(phone),
                PhoneNumber(fromNumber),
                "Your verification code is: $code\n\nThis code expires in $VERIFICATION_CODE_TTL_MINUTES minutes.",
            ).create()
        } catch (ex: Exception) {
            log.error("Failed to send verification code to {}", phone, ex)
        }
    }
}
