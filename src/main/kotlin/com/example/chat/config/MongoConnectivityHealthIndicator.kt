package com.example.chat.config

import org.springframework.boot.health.contributor.AbstractHealthIndicator
import org.springframework.boot.health.contributor.Health
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component("mongoHealthIndicator")
class MongoConnectivityHealthIndicator(
    private val mongoTemplate: MongoTemplate,
) : AbstractHealthIndicator() {

    override fun doHealthCheck(builder: Health.Builder) {
        val collections = mongoTemplate.collectionNames
        builder.up().withDetail("collections", collections.size)
    }
}
