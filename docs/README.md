# note — Kotlin + Spring Boot 4 Notes API

A production-style REST backend for a notes app, built with **Kotlin** and **Spring Boot 4**
on **MongoDB**. It ships with full JWT authentication, email verification, password reset,
rate limiting, role-based admin access, file uploads, internationalization, metrics, and a
focused automated test suite.

> Portfolio note: this project demonstrates building a secure, real-world API end to end —
> not just CRUD, but the auth, abuse-protection, and operational concerns a real service needs.

---

## ✨ What this project does

### 🔐 Authentication & security
- ✅ **JWT auth** — short-lived **access tokens** + long-lived **refresh tokens** (JJWT)
- ✅ **Token rotation** — every refresh issues a new pair and invalidates the old refresh token
- ✅ **Instant logout / revocation** — a `jti` denylist blocks already-issued access tokens
- ✅ **BCrypt password hashing** — passwords are never stored in plaintext
- ✅ **Role-based access control** — `USER` / `ADMIN` roles enforced with `@PreAuthorize`
- ✅ **Stateless security filter chain** — custom `JwtAuthFilter` + JSON 401/403 responses

### 📧 Email verification & account flows
- ✅ **Email verification** — registration sends a hashed 5-digit code; account is unverified until confirmed
- ✅ **Resend verification code** — privacy-preserving (never leaks whether an email exists)
- ✅ **Password reset** — request → verify code → set new password (all refresh tokens revoked on reset)
- ✅ **Change email** — request a change, confirm with a code sent to the *new* address
- ✅ **Async, pluggable mailer** — `log` backend for dev, `smtp` (JavaMailSender) for prod; sending never blocks the request thread
- ✅ **Hashed, expiring codes** — codes are SHA-256 hashed in the DB and auto-expire via TTL indexes

### 🚦 Abuse protection
- ✅ **Rate limiting** — token-bucket (Bucket4j) with per-endpoint limits:
  login / register / refresh limited **per IP**, other authenticated calls **per user**
- ✅ Returns proper **`429 Too Many Requests`** with a `Retry-After` header and a JSON body

### 📝 Core features
- ✅ **Notes CRUD** — create, list (paginated), fetch, update, delete
- ✅ **Ownership enforcement** — you can only read/edit/delete **your own** notes
- ✅ **Profile picture upload** — multipart upload with **magic-byte validation** (real PNG/JPEG/WebP, not a renamed file) and path-traversal protection
- ✅ **User profile** — view/update name, change password, change email

### 🌍 Platform & operations
- ✅ **Internationalization (i18n)** — every message available in **English & Arabic** via `Accept-Language`
- ✅ **Consistent response envelope** — uniform success/error JSON shape across the API
- ✅ **Global exception handling** — typed `ApiException` → clean error responses
- ✅ **OpenAPI / Swagger UI** — auto-generated API docs (springdoc)
- ✅ **Prometheus metrics** — `/actuator/prometheus` with HTTP latency histograms (p95/p99)
- ✅ **Health checks** — custom Mongo connectivity indicator + Kubernetes-style probes
- ✅ **Managed indexes** — `IndexInitializer` creates all indexes idempotently on boot, including TTL indexes for tokens/codes
- ✅ **Automated tests** — fast unit tests for all services + JWT, plus an embedded-Mongo context test

---

## 🧱 Stack

- **Kotlin 2.2** · **Spring Boot 4** · **Java 17**
- **MongoDB** (Atlas) via Spring Data
- **Spring Security** + **JJWT** for auth
- **Bucket4j** for rate limiting
- **Bean Validation** + i18n (en / ar)
- **springdoc-openapi** + **Micrometer/Prometheus**
- **JUnit 5 · MockK · Flapdoodle embedded Mongo** for testing

---

## 🌐 API endpoints

### Auth — `/api/auth`
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/register` | Create account, send verification code |
| `POST` | `/login` | Log in (returns tokens, or "verification required") |
| `POST` | `/verify-email` | Confirm the email code |
| `POST` | `/verify-email/resend` | Re-send the verification code |
| `POST` | `/password-reset/request` | Start password reset |
| `POST` | `/password-reset/verify` | Check a reset code |
| `POST` | `/password-reset/confirm` | Set the new password |
| `POST` | `/refresh` | Rotate tokens |
| `POST` | `/logout` | Revoke refresh + access tokens |

### Notes — `/api/notes`
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/` | Create note |
| `GET` | `/` | List my notes (paginated: `?page=&size=&sort=`) |
| `GET` | `/{id}` | Get one of my notes |
| `PUT` | `/{id}` | Update my note |
| `DELETE` | `/{id}` | Delete my note |

### User — `/api/user`
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/me` | My profile |
| `PATCH` | `/me` | Update name |
| `POST` | `/me/avatar` | Upload profile picture |
| `POST` | `/me/change-password` | Change password |
| `POST` | `/me/change-email/request` | Request email change |
| `POST` | `/me/change-email/verify` | Confirm email change |

### Admin — `/api/admin` (requires `ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/users` | List users (paginated) |
| `PATCH` | `/users/{id}/roles` | Update a user's roles |

### Public assets & ops
| Method | Path | Purpose |
|---|---|---|
| `GET` | `/avatars/{filename}` | Serve an avatar image |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Metrics scrape endpoint |
| `GET` | `/swagger-ui.html` | API docs |

---

## ⚙️ Profiles

| File | When |
|---|---|
| `application.yaml` | shared base |
| `application-dev.yaml` | local dev (port 8181, DEBUG logs, loose rate limits) |
| `application-prod.yaml` | production (port from `$PORT`, file logs, compressed responses) |

Switch via `SPRING_PROFILES_ACTIVE=dev|prod` (defaults to `dev`).

## 🔑 Environment variables

| Name | Required | Notes |
|---|---|---|
| `MONGODB_CONNECTION_STRING` | yes | full Atlas URI |
| `JWT_SECRET_BASE64` | yes | base64-encoded HMAC secret |
| `APP_MAILER` | no | `log` (default) or `smtp` |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | smtp only | SMTP credentials |
| `SPRING_PROFILES_ACTIVE` | no | `dev` (default) or `prod` |
| `PORT` | prod only | injected by host (Render / Railway / etc.) |
| `CORS_ALLOWED_ORIGINS` | prod only | comma-separated frontend URLs |

---

## ▶️ Run

```bash
# dev
./gradlew bootRun

# run the tests
./gradlew test

# package
./gradlew bootJar

# run prod profile locally
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/*.jar
```

---

## 🧪 Testing

Fast unit tests cover every service and the JWT logic; one embedded-Mongo test boots the
full context. See **[TESTING.md](./TESTING.md)** for the approach and how to add your own.

```bash
./gradlew test
```

---

## 🗂️ Project layout

```
auth/         register, login, verify, reset, refresh, logout, JWT, mailer, denylist
note/         CRUD + ownership checks
user/         profile, avatar upload, change password/email
admin/        list users, role management
security/     JwtAuthFilter, rate limiting, 401/403 JSON handlers
common/       ApiResponse envelope, ApiException, GlobalExceptionHandler, i18n helper
config/       Security, CORS, i18n, indexes, OpenAPI, async, custom health
resources/    YAML configs, messages.properties (en + ar), logback
```

---

## 📦 Response envelope

```json
{ "status": true,  "data": { ... } }
{ "status": false, "error": { "code": "UNAUTHORIZED", "message": "..." } }
```
