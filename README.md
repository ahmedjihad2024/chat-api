# chat — Kotlin + Spring Boot 4 Realtime Chat API

A production-style backend for a **1:1 chat app**, built with **Kotlin** and
**Spring Boot 4** on **MongoDB**. Messaging is realtime over **WebSocket/STOMP**, on
top of a full **phone-number** auth system (SMS verification), follows, JWT security,
rate limiting, role-based admin access, file uploads, internationalization, and metrics.

> This project demonstrates building a secure, realtime service end to end — JWT auth
> over both REST and WebSocket, a persist-then-push messaging model, and the
> abuse-protection and operational concerns a real service needs.

---

## ✨ Highlights

- 💬 **Realtime 1:1 chat** over WebSocket/STOMP, with a persist-then-push model so
  nothing is lost when the recipient is offline
- 📩 **Server-enforced read receipts & presence**, per-side unread counts, and a
  metadata-only inbox channel
- 🔐 **JWT auth** (access + refresh, rotation, instant revocation) across both REST and
  WebSocket, with BCrypt and role-based access
- 📱 **Phone-number auth** — SMS verification, password reset, change phone (Twilio +
  Google libphonenumber)
- 👥 **Social** — follow/unfollow, user search, profile & avatar upload
- 🚦 **Rate limiting** (Bucket4j), **i18n** (English / Arabic), **OpenAPI/Swagger**, and
  **Prometheus** metrics

---

## 🧱 Stack

**Kotlin 2.2** · **Spring Boot 4.0** · **Java 21** · **MongoDB** (Spring Data) ·
**WebSocket/STOMP** · **Spring Security + JJWT** · **Twilio** · **Bucket4j** ·
**springdoc-openapi** · **Micrometer/Prometheus** · **JUnit 5 / MockK / Flapdoodle**

---

## ▶️ Quick start

```bash
# run locally (dev profile, port 8181)
./gradlew bootRun

# run the tests
./gradlew test

# build a jar
./gradlew bootJar
```

Requires `MONGODB_CONNECTION_STRING` and `JWT_SECRET_BASE64` — see the
[full setup & env vars](docs/README.md#-environment-variables).

---

## 📚 Documentation

Detailed docs live in the [`docs/`](docs/) folder:

| Doc | What's inside |
|---|---|
| **[docs/README.md](docs/README.md)** | Full feature list, complete API reference (REST + WebSocket), profiles, env vars, run commands |
| **[docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)** | How the codebase is organized, package by package |
| **[docs/TESTING.md](docs/TESTING.md)** | Testing stack, what to test vs skip, how to run and write tests |
| **[docs/INTERVIEW-NOTES.md](docs/INTERVIEW-NOTES.md)** | Plain-language explanations of the key backend concepts used here |

---

## 🔗 Reference documentation (Spring Boot)

- [Official Gradle documentation](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin/packaging-oci-image.html)
- [Spring Security](https://docs.spring.io/spring-boot/4.0.6/reference/web/spring-security.html)
- [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
