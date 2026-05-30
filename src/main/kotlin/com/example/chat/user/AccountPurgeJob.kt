package com.example.chat.user

import com.example.chat.auth.passwordReset.PasswordResetCodeRepository
import com.example.chat.auth.repository.PhoneVerificationCodeRepository
import com.example.chat.auth.repository.RefreshTokenRepository
import com.example.chat.follows.FollowService
import com.example.chat.user.entities.User
import com.example.chat.user.repository.PhoneChangeRequestRepository
import com.example.chat.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Permanently anonymizes accounts that have been soft-deleted (see [UserService.deleteMe]) for
 * longer than [User.DELETED_RETENTION_DAYS] without the owner logging back in.
 *
 * The user document is kept as an anonymized tombstone (so existing conversations still resolve to
 * the "Deleted account" placeholder) but all personal data is stripped: name, email, password,
 * avatar and phone are cleared, and every auxiliary record keyed by the user — follows, reset and
 * verification codes, pending phone changes, refresh tokens — is hard-deleted. Messages are left
 * intact; their author is masked by the tombstone.
 *
 * The [phone] index is unique, so it is rewritten to a per-account sentinel rather than nulled,
 * which both satisfies the index and frees the real number for re-registration.
 */
@Component
class AccountPurgeJob(
    private val userRepository: UserRepository,
    private val avatarStorage: AvatarStorage,
    private val followService: FollowService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val phoneVerificationCodeRepository: PhoneVerificationCodeRepository,
    private val phoneChangeRequestRepository: PhoneChangeRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Runs daily at 03:00 server time. */
    @Scheduled(cron = "0 0 3 * * *")
    fun purgeExpired() {
        val cutoff = Instant.now().minus(User.DELETED_RETENTION_DAYS, ChronoUnit.DAYS)
        val expired = userRepository.findByDeletedTrueAndDeletedAtBefore(cutoff)
        if (expired.isEmpty()) return
        log.info(
            "Purging {} soft-deleted account(s) past the {}-day grace period",
            expired.size,
            User.DELETED_RETENTION_DAYS
        )
        expired.forEach {
            runCatching { purge(it) }.onFailure { e ->
                log.error(
                    "Failed to purge account {}",
                    it.id,
                    e
                )
            }
        }
    }

    @Transactional
    fun purge(user: User) {
        val userId = user.id
        followService.purgeUser(userId)
        refreshTokenRepository.deleteAllByUserId(userId)
        passwordResetCodeRepository.deleteByUserId(userId)
        phoneVerificationCodeRepository.deleteByUserId(userId)
        phoneChangeRequestRepository.deleteByUserId(userId)
        user.avatarFilename?.let { avatarStorage.delete(it) }

        userRepository.save(
            user.copy(
                name = User.DELETED_DISPLAY_NAME,
                phone = "deleted_${userId.toHexString()}",
                email = null,
                hashedPassword = null,
                avatarFilename = null,
                followersCount = 0,
                followingCount = 0,
            ),
        )
    }
}
