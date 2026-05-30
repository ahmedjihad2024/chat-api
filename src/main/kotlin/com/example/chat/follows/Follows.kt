package com.example.chat.follows

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "follows")
@CompoundIndex(name = "follower_followee_unique", def = "{'followerId': 1, 'followeeId': 1}", unique = true)
@CompoundIndex(name = "followee_follower", def = "{'followeeId': 1, 'followerId': 1}")
data class Follows(
    @Id val id: ObjectId = ObjectId(),
    // The user who clicked "follow" (the subscriber). Reads as: followerId follows followeeId.
    val followerId: ObjectId,
    // The user being followed (the target/subject of the follow).
    val followeeId: ObjectId,
    val createdAt: Instant = Instant.now(),
)
