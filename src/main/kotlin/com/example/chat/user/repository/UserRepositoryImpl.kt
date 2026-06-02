package com.example.chat.user.repository

import com.example.chat.user.entities.User
import com.example.chat.user.enums.Role
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

class UserRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : UserRepositoryCustom {

    override fun updateUserById(
        id: ObjectId,
        name: String?,
        phone: String?,
        email: String?,
        hashedPassword: String?,
        roles: Set<Role>?,
        phoneVerified: Boolean?,
        phoneVerifiedAt: Instant?,
        followersCountDelta: Int?,
        followingCountDelta: Int?,
        avatarFilename: String?,
        deleted: Boolean?,
        deletedAt: Instant?,
    ): User? {
        val query = Query(Criteria.where("id").`is`(id))

        val update = Update()
        name?.let { update.set("name", it) }
        phone?.let { update.set("phone", it) }
        email?.let { update.set("email", it) }
        hashedPassword?.let { update.set("hashedPassword", it) }
        roles?.let { update.set("roles", it) }
        phoneVerified?.let { update.set("phoneVerified", it) }
        phoneVerifiedAt?.let { update.set("phoneVerifiedAt", it) }
        followersCountDelta?.let { update.inc("followersCount", it) }
        followingCountDelta?.let { update.inc("followingCount", it) }
        avatarFilename?.let { update.set("avatarFilename", it) }
        deleted?.let { update.set("deleted", it) }
        deletedAt?.let { update.set("deletedAt", it) }

        // Return the document as it looks AFTER the update.
        return mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            User::class.java,
        )
    }
}
