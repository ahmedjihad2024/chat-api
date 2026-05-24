# Testing Basics

A short, practical guide to how this project is tested — and how to add your own tests.

---

## 1. Why we test

A test is a small program that runs your real code and checks it did the right thing.
It catches bugs **before** your users do, and lets you change code later without fear of
silently breaking something.

The goal is **not** "test everything." The goal is to test the parts where a bug would
actually hurt.

---

## 2. The golden rule

> **Test decisions and security. Skip plumbing.**

| Worth a test ✅ | Skip it ❌ |
|---|---|
| `if` / branches, validation, error cases | Getters, simple data classes (DTOs) |
| Authorization ("is this *your* note?") | Pure field-copying (mappers) |
| Anything involving money or permissions | Thin wrappers over the framework |
| Token generation / validation | One-line wrappers over the JDK |
| A bug you just fixed (so it never returns) | Code with no `if` and no logic |

That's why this project tests the **services** and **JWT**, but does **not** test the
mappers or the hashing helper — those just copy fields or wrap a Java library.

---

## 3. The test stack

| Tool | What it does |
|---|---|
| **JUnit 5** | The test runner (`@Test`, `@Nested`, `@BeforeEach`) |
| **MockK** | Kotlin mocking — fakes dependencies so you test one class in isolation |
| **AssertJ** | Readable assertions (`assertThat(x).isEqualTo(y)`) |
| **Flapdoodle Embedded Mongo** | Spins up an in-memory MongoDB so the full app can boot in a test, no real DB needed |

All are already configured in `build.gradle.kts` under `testImplementation`.

---

## 4. Two kinds of test in this project

### Unit tests (fast — the majority)
Test **one class** with its dependencies faked (mocked). No database, no Spring, run in
milliseconds. Live next to the code they test, e.g.:

```
src/test/kotlin/com/example/note/auth/AuthServiceTest.kt
src/test/kotlin/com/example/note/note/NoteServiceTest.kt
src/test/kotlin/com/example/note/admin/AdminServiceTest.kt
src/test/kotlin/com/example/note/user/UserServiceTest.kt
src/test/kotlin/com/example/note/security/jwt/JwtServiceTest.kt
```

### Integration test (slow — keep it to a few)
`NoteApplicationTests` boots the **whole Spring context** against an embedded MongoDB to
prove everything wires together. The first run downloads the MongoDB binary (one time,
then cached).

---

## 5. How to run them

```bash
# everything
./gradlew test

# one class
./gradlew test --tests "com.example.chat.auth.AuthServiceTest"

# one package
./gradlew test --tests "com.example.chat.note.*"
```

The HTML report is written to `build/reports/tests/test/index.html`.

---

## 6. The shape of a good test: Arrange → Act → Assert

```kotlin
@Test
fun `rejects a wrong password`() {
    // Arrange — set up the world
    val user = Fixtures.user()
    every { userRepository.findByEmail(user.email) } returns user
    every { passwordEncoder.matches("wrong", user.hashedPassword!!) } returns false

    // Act + Assert — call the code and check the outcome
    assertThatThrownBy { service.login(user.email, "wrong") }
        .isInstanceOf(ApiException.Unauthorized::class.java)
        .hasMessage("error.auth.invalid_password")
}
```

- **One behavior per test.** The name says exactly what it checks.
- **Backtick names** read like sentences: `` `rejects a wrong password` ``.

---

## 7. Mocking with MockK (the 3 things you need)

```kotlin
// 1. Create a fake of a dependency
val repo = mockk<UserRepository>(relaxed = true)

// 2. Tell it what to return when called a certain way
every { repo.findByEmail("a@b.com") } returns Fixtures.user()

// 3. Verify a call happened (or did not)
verify { repo.delete(any()) }
verify(exactly = 0) { mailer.sendVerificationCode(any(), any()) }
```

Capture what was saved to assert on it:

```kotlin
val saved = slot<User>()
every { repo.save(capture(saved)) } answers { saved.captured }
// ... call the service ...
assertThat(saved.captured.emailVerified).isTrue()
```

> **Gotcha:** with a `relaxed` mock, Spring Data's generic `save(S): S` returns a bare
> `Object` and fails the cast. Always stub it to echo the argument:
> `every { repo.save(any()) } answers { firstArg() }`

---

## 8. Shared test helpers

- **`support/Fixtures.kt`** — builds a ready-made `User`/`Note` in one line, with defaults
  you can override: `Fixtures.user(emailVerified = false)`.
- **`support/Translations.kt`** — `initTranslations()` makes `String.tr()` work in a unit
  test (no Spring context to wire up the message source).

---

## 9. Adding a test for a new feature

1. Find the **decision** in your new code (the `if`, the thrown exception, the permission).
2. Create `XxxServiceTest.kt` next to it.
3. Mock its dependencies, write one `@Test` per branch (happy path + each failure).
4. Run `./gradlew test`.

That's it. Most features need 3–6 small unit tests, not dozens.

---

## 10. Scaling to a bigger app

When an app has many endpoints, **don't write one big test per endpoint**:

1. Put the logic in **services** and unit-test those (90% of the value, cheap).
2. For controllers, write a **few** `@WebMvcTest` examples that prove the *patterns*
   (auth blocks anonymous users, bad input → 400). Once proven, you trust the pattern.
3. Keep full `@SpringBootTest` integration tests to a **handful** of critical flows
   (e.g. register → verify → login), not every route.
4. Use `@ParameterizedTest` to collapse many similar input cases into one method.

> Test where a bug would **hurt**, or where the logic is **not obvious**.
