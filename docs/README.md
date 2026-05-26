# chat — Kotlin + Spring Boot 4 Realtime Chat API

A production-style backend for a **1:1 chat app**, built with **Kotlin** and **Spring Boot 4**
on **MongoDB**. Messaging is realtime over **WebSocket/STOMP**, on top of a full
**phone-number** auth system (SMS verification), follows, JWT security, rate limiting,
role-based admin access, file uploads, internationalization, and metrics.

> Portfolio note: this project demonstrates building a secure, realtime service end to end —
> JWT auth over both REST and WebSocket, a persist-then-push messaging model, and the
> abuse-protection and operational concerns a real service needs.

---

## ✨ What this project does

### 💬 Realtime chat (the core)
- ✅ **1:1 conversations** — get-or-create a thread atomically per user pair (no duplicates)
- ✅ **WebSocket/STOMP live messaging** — send/receive over a STOMP socket; the JWT is
  authenticated on the STOMP `CONNECT` frame
- ✅ **Persist-then-push** — every message is saved (source of truth) before a best-effort
  live push, so nothing is lost when the recipient is offline
- ✅ **Read receipts, enforced server-side** — subscribing to a conversation's destination
  *is* what marks it read; the author gets a live `ReadReceipt`. A client can't read live
  without being seen.
- ✅ **Per-side unread counts** — stored per participant, surfaced as a badge in the list
- ✅ **Inbox vs conversation split** — a metadata-only `/inbox` channel updates the
  conversation list (preview, no read), full messages go to a per-conversation channel
- ✅ **Offline catch-up** — paged message history endpoint returns everything in the DB

### 🔐 Authentication & security
- ✅ **Phone-number auth** — register with a phone, confirm a 5-digit SMS code
- ✅ **JWT auth** — short-lived **access** + long-lived **refresh** tokens (JJWT), on REST and WebSocket
- ✅ **Token rotation** — every refresh issues a new pair and invalidates the old refresh token
- ✅ **Instant logout / revocation** — a `jti` denylist blocks already-issued access tokens
- ✅ **BCrypt** password hashing · **role-based access** (`USER`/`ADMIN`) via `@PreAuthorize`
- ✅ **Stateless filter chain** — custom `JwtAuthFilter` + JSON 401/403, plus a STOMP `ChannelInterceptor`

### 📱 Account flows (SMS)
- ✅ **Phone verification** — registration sends a hashed 5-digit code; account unverified until confirmed
- ✅ **Resend code** · **password reset** (request → verify → confirm) · **change phone** (verify new number)
- ✅ **Pluggable SMS sender** — `log` backend for dev (code printed to logs), **Twilio** for prod
- ✅ **Hashed, expiring codes** — SHA-256 in the DB, auto-expire via TTL indexes
- ✅ **Real phone validation** — Google libphonenumber validates + normalizes to E.164

### 👥 Social
- ✅ **Follow / unfollow** (toggle) — followers & following lists, for yourself and other users
- ✅ **User search** — find users by name to start a chat
- ✅ **Profile** — view/update name & email, change password, change phone, avatar upload

### 🚦 Abuse protection
- ✅ **Rate limiting** — token-bucket (Bucket4j): login/register/refresh per IP, other authenticated calls per user
- ✅ Proper **`429 Too Many Requests`** with `Retry-After` and a JSON body

### 🌍 Platform & operations
- ✅ **i18n** — every message in **English & Arabic** via `Accept-Language`
- ✅ **Consistent response envelope** + typed `ApiException` → clean errors (REST and STOMP error frames)
- ✅ **OpenAPI / Swagger UI** (dev only) · **Prometheus metrics** (p95/p99 latency)
- ✅ **Health probes** + **managed indexes** (`IndexInitializer`, incl. TTL indexes for tokens/codes)

---

## 🧱 Stack

- **Kotlin 2.2** · **Spring Boot 4.0** · **Java 21**
- **MongoDB** via Spring Data (**blocking** driver + `MongoTemplate`)
- **Spring WebSocket / STOMP** (in-memory simple broker) for realtime messaging
- **Spring Security** + **JJWT 0.12** for auth (REST + WebSocket)
- **Twilio** (SMS) · **Google libphonenumber** (phone validation)
- **Bucket4j** rate limiting · **Bean Validation** + i18n (en / ar)
- **springdoc-openapi** · **Micrometer / Prometheus**
- **JUnit 5 · MockK · Flapdoodle embedded Mongo** for testing

---

## 🌐 API surface

### Auth — `/api/auth`
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/register` | Create account, send SMS verification code |
| `POST` | `/login` | Log in by phone + password (tokens, or "verification required") |
| `POST` | `/verify-phone` | Confirm the SMS code |
| `POST` | `/verify-phone/resend` | Re-send the code |
| `POST` | `/password-reset/request` · `/verify` · `/confirm` | Reset flow |
| `POST` | `/refresh` | Rotate tokens |
| `POST` | `/logout` | Revoke refresh + access tokens |

### Chat — REST `/api/chats`
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/chats` | My conversations (paged, newest-active first, with unread count) |
| `GET` | `/api/chats/{conversationId}/messages` | Paged history (also marks the thread read) |

> Conversations are get-or-created automatically when you send the first message over the socket.
> A manual "open conversation", per-side read-status, and manual mark-read also exist in the
> service layer (REST handlers can be toggled on in `ChatController`).

### Chat — WebSocket / STOMP `/ws`
| Direction | Destination | Payload |
|---|---|---|
| connect | `CONNECT` frame with `Authorization: Bearer <jwt>` | — |
| send | `/app/chat.send` | `{ recipientId, text }` |
| subscribe | `/user/queue/inbox` | `InboxEvent` (conversation-list metadata) |
| subscribe | `/user/queue/conv/{conversationId}` | full `MessageResponse` (subscribing = open chat = mark read) |
| subscribe | `/user/queue/read` | `ReadReceipt` (your messages were read) |
| subscribe | `/user/queue/errors` | error envelope for a failed send |

### User — `/api/user`
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/me` | My profile |
| `GET` | `/search?q=` | Search users by name (paged) |
| `PATCH` | `/me` | Update name / email |
| `POST` | `/me/avatar` | Upload profile picture (multipart) |
| `POST` | `/me/change-password` | Change password |
| `POST` | `/me/change-phone/request` · `/verify` | Change phone (SMS verified) |

### Follows — `/api/follows`
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/follows/toggle-following/{id}` | Follow / unfollow a user |
| `GET` | `/follows/followers` · `/following` | My lists (paged) |
| `GET` | `/follows/followers/{id}` · `/following/{id}` | Another user's lists |

### Admin — `/api/admin` (requires `ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/users` | List users (paged) |
| `PATCH` | `/users/{id}/roles` | Update a user's roles |

### Public assets & ops
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/avatars/{filename}` | Serve an avatar image |
| `GET` | `/actuator/health` · `/actuator/prometheus` | Health / metrics |
| `GET` | `/swagger-ui.html` | API docs (dev only) |

---

## ⚙️ Profiles

| File | When |
|---|---|
| `application.yaml` | shared base (Mongo, JWT, i18n, pagination, rate limits, Twilio, actuator) |
| `application-dev.yaml` | local dev (port **8181**, DEBUG logs, loose rate limits, `sms=log`) |
| `application-prod.yaml` | production (port from `$PORT`, quiet logs, gzip, `sms=twilio`, Swagger off) |

Switch via `SPRING_PROFILES_ACTIVE=dev|prod` (defaults to `dev`).

## 🔑 Environment variables

| Name | Required | Notes |
|---|---|---|
| `MONGODB_CONNECTION_STRING` | yes | full Mongo URI |
| `JWT_SECRET_BASE64` | yes | base64-encoded HMAC secret |
| `APP_SMS` | no | `log` (default) or `twilio` |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | twilio only | SMS credentials |
| `ADMIN_PHONES` | no | comma-separated E.164 numbers granted `ADMIN` |
| `AVATAR_DIR` | no | avatar storage dir (default `uploads/avatars`) |
| `SPRING_PROFILES_ACTIVE` | no | `dev` (default) or `prod` |
| `PORT` | prod only | injected by host |
| `CORS_ALLOWED_ORIGINS` | prod only | comma-separated frontend URLs (browser clients only) |

---

## ▶️ Run

```bash
# dev (port 8181)
./gradlew bootRun

# tests
./gradlew test

# package + run prod profile
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/*.jar
```

---

## 🧪 Testing

Fast unit tests cover the services + JWT; one embedded-Mongo test boots the full context.
See **[TESTING.md](./TESTING.md)**.

```bash
./gradlew test
```

For trying the API by hand, import **`chat-api.postman_collection.json`** (REST), and use a
STOMP client for the WebSocket layer (Postman can only send STOMP frames in Binary mode — see
the collection's WebSocket folder, or `../tools/ws-test.js`).

---

## 🗂️ Project layout

```
auth/         register, login, verify-phone, reset, refresh, logout, JWT, SMS, denylist
chat/         conversations + messages, REST + STOMP controllers, read-receipt/presence
follows/      follow/unfollow, followers & following lists
user/         profile, search, avatar upload, change password/phone
admin/        list users, role management
security/     JwtAuthFilter, rate limiting, 401/403 JSON handlers
common/       ApiResponse envelope, ApiException, i18n helper, phone validation
config/       Security, WebSocket (+ auth interceptor & presence listener), CORS, indexes, OpenAPI
resources/    YAML configs, messages.properties (en + ar)
```

---

## 📦 Response envelope

```json
{ "status": true,  "data": { ... } }
{ "status": false, "error": { "code": "UNAUTHORIZED", "message": "..." } }
```
