plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Builds REST APIs over Spring MVC with embedded Tomcat, Jackson JSON, and servlet support.
	implementation("org.springframework.boot:spring-boot-starter-web")

	// STOMP-over-WebSocket messaging: @MessageMapping controllers, the in-memory simple broker,
	// and SimpMessagingTemplate for pushing live chat messages to connected clients.
	implementation("org.springframework.boot:spring-boot-starter-websocket")

//	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive") // reactive

	// Blocking MongoDB integration via Spring Data: repositories, MongoTemplate, and document mapping.
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

	// Spring Security core: authentication, authorization, filter chain, CSRF, and method security.
	implementation("org.springframework.boot:spring-boot-starter-security")

	// Lightweight Spring Security crypto module (BCrypt/PBKDF2/Argon2 password encoders, key generators).
	implementation("org.springframework.security:spring-security-crypto")

	// Bean Validation (Jakarta Validation / Hibernate Validator) for @Valid, @NotNull, @Email, etc.
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Kotlin extension functions for Project Reactor (idiomatic Flux/Mono usage from Kotlin).
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	// Kotlin reflection runtime — required by Spring/Jackson to inspect Kotlin classes and data classes.
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Bridge between Kotlin coroutines and Reactor (`await*`, `asFlow`, `mono { }`, `flux { }`).
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Spring Boot test stack: JUnit 5, AssertJ, Mockito, MockMvc, JsonPath, Spring Test.
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// StepVerifier and test utilities for asserting on reactive Publisher streams.
	testImplementation("io.projectreactor:reactor-test")

	// Kotlin test assertions wired to JUnit 5 (`kotlin.test` API on the Jupiter engine).
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

	// Tools for testing coroutines: `runTest`, virtual time, test dispatchers.
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

	// Spring Security test helpers — `@WithMockUser`, security-aware MockMvc/WebTestClient mutators.
	testImplementation("org.springframework.security:spring-security-test")

	// MockK — Kotlin-native mocking library (mocks final classes/data classes, relaxed mocks,
	// expressive every/verify DSL). Used for fast, isolated service unit tests.
	testImplementation("io.mockk:mockk:1.13.13")

	// Flapdoodle embedded MongoDB (Spring Boot 4.x module) — boots an in-memory mongod for
	// @SpringBootTest so the full application context loads without a real database or Docker.
	testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring4x:4.24.0")

	// JUnit Platform launcher required at test runtime by newer Gradle/Surefire versions.
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Jakarta Servlet API — compile-only because the embedded servlet container provides it at runtime.
	compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

	// JJWT public API for building and parsing JSON Web Tokens (JWS/JWE/JWT claims).
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")

	// JJWT runtime implementation of the API above (signing/parsing engine).
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")

	// JJWT Jackson integration for (de)serializing JWT claims as JSON.
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Spring Boot Actuator — exposes production-ready endpoints (health, info, metrics).
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Bucket4j core — token-bucket rate limiting library (used by RateLimitFilter).
	implementation("com.bucket4j:bucket4j-core:8.10.1")

	// Caffeine — high-performance in-process cache used by RateLimitFilter to bound the
	// bucket map (TTL + max size) so unique IPs/users can't grow it without limit.
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

	// Springdoc OpenAPI — auto-generates an OpenAPI 3 spec from controllers and serves Swagger UI.
	// Exposes /v3/api-docs (JSON spec, importable into Postman) and /swagger-ui.html.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	// Micrometer Prometheus registry — turns the Actuator metrics into a
	// /actuator/prometheus endpoint that Prometheus can scrape every ~15s.
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	// Twilio SDK — sends SMS verification codes in production (TwilioSmsSender).
	// A LogSmsSender impl is used in dev/test, so Twilio credentials are only
	// required when app.sms=twilio.
	// Exclude Twilio's transitive jjwt 0.11.2: we only use Twilio's REST (SMS) API,
	// not its JWT helpers, and the old jjwt clashes with our 0.12.6 (NoSuchMethodError
	// in io.jsonwebtoken.lang.Assert when parsing tokens).
	implementation("com.twilio.sdk:twilio:10.6.4") {
		exclude(group = "io.jsonwebtoken")
	}

	// Google libphonenumber — validates that a phone number is actually possible/valid
	// for its country (not just E.164-shaped) and normalizes input to canonical E.164.
	implementation("com.googlecode.libphonenumber:libphonenumber:8.13.52")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
