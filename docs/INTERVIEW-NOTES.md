# Backend Concepts — Interview Notes

Plain-language explanations of the key terms used across the **chat** and **note**
Kotlin + Spring Boot projects. Useful for CV review and interview prep.

---

## CRUD

**C**reate, **R**ead, **U**pdate, **D**elete — the four basic operations on data.

In the notes app: create a note, read/list notes, edit a note, delete a note.
"Goes beyond CRUD" means the project adds auth, email, rate limiting, etc. on top
of these basics.

---

## Revocation (Auth & Security)

Normally a JWT is valid until it expires, and the server can't "take it back."
**Revocation** is a way to **kill a token before it expires**.

When a user logs out, the token's ID (`jti`) is added to a **denylist**, so even
though the token still looks valid, the server rejects it. Without this, logout
wouldn't really log anyone out.

---

## Spring Data (Backend)

A Spring library that handles the database. Instead of writing raw MongoDB queries,
you declare an interface method like `findByEmail(email)` and Spring Data
**generates the query automatically**. It removes most database boilerplate.

---

## Bean Validation

Automatic checking of incoming request data **before** your code runs. You annotate
fields:

```kotlin
@field:NotBlank @field:Email val email: String
@field:Size(max = 4000) val text: String
```

If a request breaks a rule (empty email, text too long), Spring rejects it with an
error automatically. ("Bean" = an object that Spring manages.)

---

## TTL — Time To Live (Data)

A MongoDB index that **auto-deletes documents after a set time**.

Used for SMS / verification codes — they expire and get cleaned up automatically
(e.g. after 10 minutes) instead of piling up in the database forever.

---

## Multipart Uploads (Integration)

The way files (not just text/JSON) are sent over HTTP. When a user uploads an avatar
image, the request is `multipart/form-data` — it carries the binary file. The backend
receives and saves it. Normal JSON requests can't carry files; multipart can.

---

## Embedded MongoDB — Flapdoodle (Testing)

A real MongoDB that **starts up inside your test** and disappears when tests finish.
You can test the whole app against a genuine database **without installing MongoDB or
running Docker**. Flapdoodle is the library that downloads and boots this temporary
in-memory mongo.

---

## OpenAPI vs Swagger

People use them interchangeably, but technically:

- **OpenAPI** = the **specification** — the standard format (a JSON/YAML file) that
  describes all endpoints, params, and responses.
- **Swagger** = the **tooling** around it — most importantly **Swagger UI**, the
  interactive web page where you can see and try the endpoints in a browser.

So: OpenAPI is the *document*, Swagger UI is the *interactive viewer* built from it.
In these projects, `springdoc` reads the controllers, generates the OpenAPI spec, and
serves Swagger UI from it.
