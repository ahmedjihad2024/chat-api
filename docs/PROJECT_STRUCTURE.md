# Project Structure Guide

Package layout for this **Spring Boot 4 + Kotlin + MongoDB (blocking) + WebSocket/STOMP + JWT**
realtime chat backend.

## Approach: Package-by-Feature

Group code by **business feature** (auth, chat, follows, user...) instead of by technical layer
(controllers/, services/, repositories/).

**Why:**
- When you work on "chat," everything is in one folder.
- Features can be added/removed without touching unrelated code.
- Scales better than layered as the project grows.
- Easier onboarding — new devs read one feature top-to-bottom.

---

## Full Layout

```
com.example.chat
│
├── ChatApplication.kt                       # @SpringBootApplication entry point
│
├── config/                                  # Cross-cutting Spring configuration
│   ├── SecurityConfig.kt                    # SecurityFilterChain, BCrypt, CORS, method security
│   ├── WebSocketConfig.kt                   # @EnableWebSocketMessageBroker: /ws endpoint, broker, prefixes
│   ├── WebSocketAuthInterceptor.kt          # ChannelInterceptor — authenticates the STOMP CONNECT frame
│   ├── WebSocketPresenceListener.kt         # SUBSCRIBE/UNSUBSCRIBE/DISCONNECT → viewing presence + mark read
│   ├── IndexInitializer.kt                  # Creates Mongo indexes (incl. TTL) idempotently on boot
│   └── OpenApiConfig.kt                     # Swagger / springdoc setup
│
├── common/                                  # Shared building blocks (NO domain logic)
│   ├── exception/
│   │   ├── ApiException.kt                  # Sealed app errors (NotFound, BadRequest, Unauthorized...)
│   │   └── GlobalExceptionHandler.kt        # @RestControllerAdvice — maps exceptions → HTTP
│   ├── dto/
│   │   └── ApiResponse.kt                   # Standard envelope { status, data, error, meta }
│   ├── phone/                               # E.164 validation + deserialization (libphonenumber)
│   └── extentions/                          # tr() i18n helper, etc.
│
├── security/                                # Auth crosses every feature, so it lives on its own
│   ├── jwt/
│   │   ├── JwtService.kt                    # Build & parse JWTs (jjwt 0.12)
│   │   └── JwtAuthFilter.kt                 # OncePerRequestFilter that validates Bearer tokens
│   └── ratelimit/                           # Bucket4j RateLimitFilter + properties
│
├── auth/                                    # FEATURE — phone-number auth
│   ├── AuthController.kt · AuthService.kt
│   ├── sms/                                 # SmsSender (LogSmsSender dev, TwilioSmsSender prod)
│   ├── passwordReset/                       # reset code entity/repo/dto
│   ├── entities/  repository/  dto/         # RefreshToken, RevokedAccessToken, PhoneVerificationCode
│
├── chat/                                    # FEATURE — realtime messaging
│   ├── ChatController.kt                    # REST: list conversations, history
│   ├── ChatSocketController.kt              # STOMP: @MessageMapping("chat.send") + fan-out
│   ├── ChatService.kt                       # send/read/history business logic
│   ├── ChatNotifier.kt                      # wraps SimpMessagingTemplate (message/inbox/readReceipt)
│   ├── ActiveConversationRegistry.kt        # who is viewing which conversation (drives mark-read)
│   ├── entities/        Conversation.kt, Message.kt
│   ├── repository/      Mongo repos + custom fragments (findOrCreate, applySentMessage, markIncomingRead)
│   ├── dto/             MessageResponse, ConversationResponse, InboxEvent, ReadReceipt, ...
│   └── mapper/          toResponse() extension functions
│
├── follows/                                 # FEATURE — follow/unfollow, followers/following
│   ├── FollowController.kt · FollowService.kt · entities/ repository/ dto/
│
├── user/                                    # FEATURE — profile, search, avatar
│   ├── UserController.kt · AvatarController.kt · UserService.kt · AvatarStorage.kt
│   ├── entities/ repository/ dto/ mapper/ enums/ (Role)
│
└── admin/                                   # FEATURE — role-gated user management
    └── AdminController.kt · AdminService.kt · dto/
```

---

## Resources Layout

```
src/main/resources/
├── application.yaml                         # shared base
├── application-dev.yaml                     # Profile: dev (port 8181, sms=log)
├── application-prod.yaml                    # Profile: prod (sms=twilio, Swagger off)
├── messages.properties                      # i18n (English)
└── messages_ar.properties                   # i18n (Arabic)
```

---

## Rules of Thumb

### 1. Never expose entities through controllers
Always use DTOs. `@Document` classes are persistence concerns; DTOs are API concerns.

```kotlin
// GOOD
@GetMapping("/me")
fun me(@AuthenticationPrincipal id: String): ApiResponse<UserResponse> = userService.me(id)
```

### 2. Data access lives in the repository, logic in the service
Derived queries / `@Query` for simple cases; **custom fragments** (`...RepositoryImpl` with
`MongoTemplate`) for dynamic updates like the per-participant `unread` map. The service speaks
intent (`conversationRepository.applySentMessage(...)`), the repo owns the Mongo details.

### 3. Sealed exceptions + global handler
```kotlin
sealed class ApiException(message: String) : RuntimeException(message) {
    class NotFound(messageKey: String) : ApiException(messageKey)
    class BadRequest(messageKey: String) : ApiException(messageKey)
}
```
`@RestControllerAdvice` maps each subtype to the right HTTP status; STOMP errors are mirrored
to `/user/queue/errors` by `ChatSocketController`.

### 4. Mappers as extension functions
```kotlin
fun Message.toResponse() = MessageResponse(id = id.toHexString(), ...)
```

### 5. Layer dependencies — one-way
```
Controller → Service → Repository → Entity
                ↓
              DTO (in/out)
```
- Controllers know nothing about Mongo. Repositories know nothing about DTOs.
- Services own business rules; `ChatNotifier` owns the realtime pushes.

---

## Stack-Specific Notes

### Mongo: blocking only
`build.gradle.kts` uses **`spring-boot-starter-data-mongodb`** (blocking) with Spring MVC. The
reactive starter is intentionally commented out — this is a blocking, thread-per-request app, so
**coroutines/WebFlux are not used** (they'd add complexity with no benefit over a blocking
driver). Go reactive only if you later need to handle very high concurrent connection counts.

### WebSocket/STOMP
- `/ws` handshake is **permit-all**; the JWT is authenticated on the STOMP `CONNECT` frame by
  `WebSocketAuthInterceptor` (a `ChannelInterceptor`), which sets the user id as the session principal.
- Per-user delivery uses the `/user` prefix + `convertAndSendToUser`.
- "Viewing" presence (for read receipts) is derived from SUBSCRIBE/UNSUBSCRIBE frames in
  `WebSocketPresenceListener` — never from a client-sent message.
- The simple broker and `ActiveConversationRegistry` are **in-memory / single-instance**; scaling
  horizontally needs a shared broker + shared presence store.

### JWT (jjwt 0.12.x)
`JwtService` exposes `generateAccessToken`, `generateRefreshToken`, `validateAccessToken`,
`getUserIdFromToken`, `getJti`. The secret comes from `JWT_SECRET_BASE64` — never hard-coded.

### Validation
```kotlin
data class CreateMessageRequest(
    @field:NotBlank val recipientId: String,
    @field:NotBlank @field:Size(max = 4000) val text: String,
)
```
Controller / `@MessageMapping`: `fun send(@Valid @Payload body: CreateMessageRequest, ...)`.

---

## Quick Checklist When Adding a New Feature

1. Create the package: `com.example.chat.<feature>/`
2. Add `entities/<Feature>.kt`, `repository/<Feature>Repository.kt`
3. Add `dto/` with request + response classes
4. Add `<Feature>Service.kt` — all business logic
5. Add `<Feature>Controller.kt` — thin HTTP/STOMP + delegation
6. Add `mapper/` extension functions
7. Wire security rules in `config/SecurityConfig.kt` (and `WebSocketConfig` if it has socket destinations)
8. Write tests under `src/test/kotlin/com/example/chat/<feature>/`
