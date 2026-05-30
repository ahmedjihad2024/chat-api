package com.example.chat.user.entities

import com.example.chat.user.enums.Role
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("users")
data class User(
    val name: String,
    @Indexed(unique = true) val phone: String,
    @Indexed(unique = true, sparse = true) val email: String? = null,
    val hashedPassword: String?,
    val roles: Set<Role> = setOf(Role.USER),
    val phoneVerified: Boolean = false,
    val phoneVerifiedAt: Instant? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val avatarFilename: String? = null,
    /** Soft-delete flag. While true the account is hidden from everyone else but still fully
     *  recoverable: logging back in clears it. After [DELETED_RETENTION_DAYS] with no login the
     *  scheduled purge anonymizes the document permanently. */
    val deleted: Boolean = false,
    val deletedAt: Instant? = null,
    @Id val id: ObjectId = ObjectId()
) {
    companion object {
        /** Grace period a soft-deleted account is kept before it is anonymized. */
        const val DELETED_RETENTION_DAYS = 30L

        /** Name shown to other users in place of a deleted account. */
        const val DELETED_DISPLAY_NAME = "Deleted account"
    }
}
