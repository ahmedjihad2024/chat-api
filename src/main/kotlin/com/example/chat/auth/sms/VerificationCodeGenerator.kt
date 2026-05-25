package com.example.chat.auth.sms

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.SecureRandom

const val VERIFICATION_CODE_TTL_MINUTES: Long = 15L

/**
 * Produces the 5-digit codes used for phone verification, password reset, and
 * phone change. The active implementation is chosen by `app.sms`, mirroring the
 * [SmsSender] beans: the `log` backend (dev) uses a fixed, predictable code so
 * you can test flows without reading logs/SMS, while the `twilio` backend (prod)
 * uses a cryptographically random code.
 */
interface VerificationCodeGenerator {
    // 5-digit code (00000..99999). Range matches the DTO regex `^\d{5}$`.
    fun generate(): String
}

/** Dev/test generator: always returns the same predictable code. */
@Component
@ConditionalOnProperty(name = ["app.sms"], havingValue = "log", matchIfMissing = true)
class FixedVerificationCodeGenerator : VerificationCodeGenerator {
    override fun generate(): String = "%05d".format(12345)
}

/** Production generator: cryptographically random code. */
@Component
@ConditionalOnProperty(name = ["app.sms"], havingValue = "twilio")
class RandomVerificationCodeGenerator : VerificationCodeGenerator {
    private val rng = SecureRandom()

    override fun generate(): String = "%05d".format(rng.nextInt(100_000))
}
