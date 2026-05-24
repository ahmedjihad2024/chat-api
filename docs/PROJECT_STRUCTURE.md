# Project Structure Guide

Recommended package layout for this Spring Boot + Kotlin + MongoDB (Reactive) + Security + JWT backend.

## Approach: Package-by-Feature

Group code by **business feature** (user, auth, order, course...) instead of by technical layer (controllers/, services/, repositories/).

**Why:**
- When you work on "users," everything is in one folder.
- Features can be added/removed without touching unrelated code.
- Scales better than layered as the project grows.
- Easier onboarding — new devs read one feature top-to-bottom.

---

## Full Layout

```
com.example.chat
│
├── StudingApplication.kt                    # @SpringBootApplication entry point
│
├── config/                                  # Cross-cutting Spring configuration
│   ├── SecurityConfig.kt                    # SecurityFilterChain, password encoder, auth manager
│   ├── MongoConfig.kt                       # Mongo client, auditing, converters
│   ├── WebConfig.kt                         # CORS, message converters, interceptors
│   └── OpenApiConfig.kt                     # Swagger / springdoc setup (optional)
│
├── common/                                  # Shared building blocks (NO domain logic)
│   ├── exception/
│   │   ├── ApiException.kt                  # Base sealed class for app errors
│   │   ├── ErrorCode.kt                     # Enum of error codes
│   │   └── GlobalExceptionHandler.kt        # @RestControllerAdvice — maps exceptions → HTTP
│   ├── dto/
│   │   └── ApiResponse.kt                   # Standard response wrapper { data, error, meta }
│   ├── audit/
│   │   └── Auditable.kt                     # Base class with @CreatedDate, @LastModifiedDate
│   └── util/                                # Pure helpers (date, string, etc.)
│
├── security/                                # Auth crosses every feature, so it lives on its own
│   ├── jwt/
│   │   ├── JwtService.kt                    # Build & parse JWTs using jjwt
│   │   ├── JwtAuthFilter.kt                 # OncePerRequestFilter that validates tokens
│   │   └── JwtProperties.kt                 # @ConfigurationProperties("app.jwt")
│   ├── UserPrincipal.kt                     # Implements UserDetails
│   └── CustomUserDetailsService.kt          # Loads users from Mongo for Spring Security
│
├── user/                                    # FEATURE
│   ├── UserController.kt                    # HTTP layer (@RestController)
│   ├── UserService.kt                       # Business logic
│   ├── UserRepository.kt                    # Spring Data Mongo interface
│   ├── User.kt                              # @Document entity
│   ├── dto/
│   │   ├── UserRequest.kt                   # Inbound DTO (with validation annotations)
│   │   └── UserResponse.kt                  # Outbound DTO (never expose entity directly)
│   └── mapper/
│       └── UserMapper.kt                    # Extension functions: User.toResponse(), etc.
│
├── auth/                                    # FEATURE (login / register / refresh)
│   ├── AuthController.kt
│   ├── AuthService.kt
│   └── dto/
│       ├── LoginRequest.kt
│       ├── RegisterRequest.kt
│       └── TokenResponse.kt
│
└── <other-features>/                        # Same pattern: order/, course/, lesson/, ...
```

---

## Resources Layout

```
src/main/resources/
├── application.yml                          # Prefer YAML over .properties
├── application-dev.yml                      # Profile: dev
├── application-prod.yml                     # Profile: prod
└── logback-spring.xml                       # Logging config
```

---

## Rules of Thumb

### 1. Never expose entities through controllers
Always use DTOs. `@Document` classes are persistence concerns; DTOs are API concerns.

```kotlin
// BAD
@GetMapping("/{id}")
fun get(@PathVariable id: String): User = service.findById(id)

// GOOD
@GetMapping("/{id}")
fun get(@PathVariable id: String): UserResponse = service.findById(id).toResponse()
```

### 2. Use `@ConfigurationProperties` for grouped settings
Beats `@Value("${...}")` for anything with more than one related property.

```kotlin
@ConfigurationProperties("app.jwt")
data class JwtProperties(
    val secret: String,
    val expiry: Duration,
    val issuer: String
)
```

### 3. Sealed exceptions + global handler
```kotlin
sealed class ApiException(message: String) : RuntimeException(message) {
    class NotFound(resource: String) : ApiException("$resource not found")
    class Unauthorized(reason: String) : ApiException(reason)
    class Conflict(reason: String) : ApiException(reason)
}
```
The `@RestControllerAdvice` maps each subtype to the right HTTP status with a `when` expression.

### 4. Mappers as extension functions
For small/medium projects, extension functions beat MapStruct in ceremony:
```kotlin
fun User.toResponse() = UserResponse(id = id, email = email, name = name)
fun RegisterRequest.toEntity() = User(email = email, passwordHash = ...)
```

### 5. Layer dependencies — keep them one-way
```
Controller → Service → Repository → Entity
                ↓
              DTO (in/out)
```
- Controllers know nothing about Mongo.
- Repositories know nothing about DTOs.
- Services own the business rules and orchestrate everything.

---

## Stack-Specific Notes

### Pick ONE Mongo style
Current `build.gradle.kts` includes **both** blocking and reactive Mongo starters:
- `spring-boot-starter-data-mongodb` (blocking)
- `spring-boot-starter-data-mongodb-reactive` (reactive)

Running both works but doubles the mental model. Pick one unless there is a clear reason for both:
- **Reactive** if the whole app is reactive (WebFlux + coroutines).
- **Blocking** if using Spring MVC and you do not need backpressure / massive concurrency.

> Note: you also have `spring-boot-starter-web` (MVC, blocking). Mixing MVC + reactive Mongo is allowed but unusual — consider going fully one way.

### JWT helpers (jjwt 0.12.x)
- `JwtService` should expose: `generateAccessToken(user)`, `generateRefreshToken(user)`, `parse(token)`, `isValid(token)`.
- Store `JwtProperties.secret` in env vars or a secrets manager — never in `application.yml` checked into git.

### Validation
With `spring-boot-starter-validation`, annotate DTO fields:
```kotlin
data class RegisterRequest(
    @field:Email val email: String,
    @field:Size(min = 8) val password: String,
    @field:NotBlank val name: String
)
```
Then in the controller: `fun register(@Valid @RequestBody body: RegisterRequest)`.

---

## When to Deviate

| If your project ... | Then consider ... |
|---|---|
| Has 50+ features | Split into Gradle modules per bounded context |
| Has complex domain rules | Add a `domain/` sub-package per feature for pure-Kotlin domain models |
| Goes hexagonal / clean | Split each feature into `domain/`, `application/`, `infrastructure/`, `api/` |
| Is a small CRUD service | The above is overkill — stick with this structure |

---

## Quick Checklist When Adding a New Feature

1. Create the package: `com.example.chat.<feature>/`
2. Add `<Feature>.kt` (entity), `<Feature>Repository.kt`
3. Add `dto/` with request + response classes
4. Add `<Feature>Service.kt` — all business logic
5. Add `<Feature>Controller.kt` — thin, just HTTP + delegation
6. Add `mapper/` extension functions
7. Wire up security rules in `config/SecurityConfig.kt` if endpoints need protection
8. Write tests next to the feature: `src/test/kotlin/com/example/studing/<feature>/`
