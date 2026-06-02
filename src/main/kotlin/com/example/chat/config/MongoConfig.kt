package com.example.chat.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Enables multi-document ACID transactions for MongoDB.
 *
 * Registering a [MongoTransactionManager] is what makes Spring's `@Transactional` actually open a
 * Mongo session and commit/roll back every operation inside the annotated method as a unit. Without
 * this bean `@Transactional` is a silent no-op for Mongo and each write commits independently.
 *
 * REQUIRES a replica set (or sharded cluster): standalone `mongod` cannot run transactions and will
 * throw at runtime. Atlas and managed providers are already replica sets; a bare local `mongod` must
 * be started with `--replSet` (a single-node `rs0` is enough for development).
 */
@Configuration
@EnableTransactionManagement
class MongoConfig {

    @Bean
    fun mongoTransactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager =
        MongoTransactionManager(dbFactory)
}
