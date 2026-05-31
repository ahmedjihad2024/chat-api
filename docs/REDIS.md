# Redis in this project — explained

How Redis is used to make the chat backend **multi-instance ready** (safe to run 2+ replicas), and why
different features use different Redis tools.

---

## 1. The Redis "package" — Spring Data Redis

Adding `spring-boot-starter-data-redis` gives you one main tool: **`StringRedisTemplate`** (and its parent
`RedisTemplate`). This is your **remote control for Redis** — a Kotlin object whose methods map to Redis
commands. You inject it and call methods; under the hood it talks to the Redis server over the network.

Redis stores different **data types** (string, set, hash, list, sorted-set...). The template gives you a
sub-object per type, via `opsForX()`:

| Method | Redis type | Think of it as |
|---|---|---|
| `redis.opsForValue()` | string | a single value (`SET`/`GET`) |
| `redis.opsForSet()` | set | a bag of unique items |
| `redis.opsForHash()` | hash | a mini key→value map |
| `redis.opsForList()` | list | a queue/stack |

So `opsForSet()` = "give me the commands that operate on Redis **sets**." `opsForHash()` = the commands for
**hashes**. That's all `opsForX()` means.

---

## 2. `redis.opsForSet().add(key, value)` — line by line

This is in **`PresenceRegistry`** (not the rate limiter). The actual call:

```kotlin
val added = redis.opsForSet().add(k, sessionId) ?: 0
```

- `redis` → the `StringRedisTemplate` (the remote control).
- `.opsForSet()` → "I want to work with a **Redis Set**."
- `.add(k, sessionId)` → run the Redis command **`SADD k sessionId`** = "add `sessionId` into the set stored
  at key `k`."
  - `k` is `"presence:user:{userId}"` — one set per user.
  - `sessionId` is the member being added.
- **Return value** = how many *new* members were added (Redis `SADD` returns this):
  - `1` → this session was new → user **just came online** (set went 0→1).
  - `0` → that session was already there (a duplicate) → no edge.
- `?: 0` → Kotlin null-safety: the method returns `Long?`, so if it's `null` we treat it as `0`.

**Why a Set for presence?** A user can have multiple devices/sockets, and a set:
1. holds many session ids per user,
2. auto-deduplicates,
3. lets us ask "how many sessions?" with `SCARD` (`.size()`),
4. lets us detect the **edges** (0→1 = online, →0 = offline).

That's exactly the multi-device online/offline logic.

---

## 3. Why the rate limiter does NOT use `opsForSet` — it uses bucket4j

Key idea: **rate limiting is not a "store a value" problem — it's an algorithm.**

The **token-bucket algorithm** must, on *every* request, atomically:
1. calculate how many tokens have refilled since the last request (based on elapsed time),
2. check if ≥1 token is available,
3. subtract one,
4. save the new state,

…and do all of that **without race conditions**, even when many requests hit different instances at the same
millisecond.

Doing that by hand with `opsForValue()`/`opsForSet()` would mean read → compute refill → write back, while
somehow stopping two simultaneous requests from both reading "1 token left" and both consuming it. That's a
hard, bug-prone distributed-locking problem. **bucket4j already solved it** — it ships Lua scripts that run
*inside* Redis atomically. So we hand the job to bucket4j's `proxyManager`:

```kotlin
val bucket = proxyManager.builder()
    .build(resolution.key) { bucketConfig(resolution.rule) }
val probe = bucket.tryConsumeAndReturnRemaining(1)
```

- `proxyManager` → bucket4j's Redis-backed manager (created in `RedisConfig`). *It* talks to Redis for us.
- `.builder().build(key) { bucketConfig(rule) }` → "give me the bucket stored at `key`; if it doesn't exist
  yet, create it with this config (capacity + refill rate)."
- `tryConsumeAndReturnRemaining(1)` → atomically try to take 1 token; tells us if it succeeded and how many
  remain.

The distinction:

| | Tool | Why |
|---|---|---|
| **Presence / viewing** | `StringRedisTemplate` (`opsForSet`/`opsForHash`) | Simple "remember these members" — basic data ops are enough |
| **Rate limiting** | bucket4j `proxyManager` | Needs an atomic time-based algorithm — a specialized library does it correctly |

We *could* technically build rate limiting on raw `opsForValue`, but it would be incorrect under concurrency.
bucket4j is the right abstraction — it just happens to use the same Redis as its storage underneath.

---

## 4. Tour of every edit

**`build.gradle.kts`**
- Added `spring-boot-starter-data-redis` (the `StringRedisTemplate` + Lettuce client).
- Added `bucket4j-redis` (the distributed bucket store).
- Removed `caffeine` (the old in-memory bucket map — no longer needed).
- Added `embedded-redis` for tests.

**`application.yaml`** — added `spring.data.redis.url` (defaults to local, `REDIS_URL` in prod) so the
template knows where Redis is.

**`config/RedisConfig.kt`** (new) — wires two things Spring can't infer:
1. the bucket4j `proxyManager` (with its own Lettuce connection + 10-min idle expiry),
2. the `RedisMessageListenerContainer` that subscribes each instance to the `chat:live` channel.

**`security/ratelimit/RateLimitFilter.kt`** — swapped the Caffeine map for the `proxyManager` (section 3) +
**fail-open** try/catch so a Redis outage doesn't block users.

**`chat/PresenceRegistry.kt`** — `ConcurrentHashMap` → Redis **set** per user (`opsForSet`), plus a
`@Scheduled` TTL refresh so crashed instances self-heal.

**`chat/ActiveConversationRegistry.kt`** — moved only the shared "who's viewing conversation X" aggregate to
a Redis **hash** (`opsForHash`, with per-user counts via `HINCRBY`); kept per-socket bookkeeping local.

**`config/socket/WebSocketPresenceListener.kt`** — now passes `sessionId` into presence (the set needs a
member to add/remove).

**`chat/ChatNotifier.kt`** — instead of pushing straight to the local broker, it now **publishes to Redis**
(`RedisLivePublisher`) so any instance can deliver.

**`chat/live/` (new)** — `LiveEnvelope` (the message wrapper), `RedisLivePublisher` (publishes),
`RedisLiveSubscriber` (each instance receives + delivers locally).

**`docker-compose.yml` (new)** — local Redis for dev.

**`ChatApplicationTests.kt`** — starts an embedded Redis so the context test still runs without Docker.

---

## One-line summary

`opsForSet`/`opsForHash` are for *simple "remember this" storage* (presence/viewing); bucket4j's
`proxyManager` is for the *atomic time-based counting algorithm* (rate limiting) — both sit on the same
Redis, just at different levels of abstraction.

- Official Spring docs:
  https://docs.spring.io/spring-data/redis/reference/ — covers
  RedisTemplate, opsForSet/Value/Hash, serializers, pub/sub.
- Spring guide (hands-on):
  https://spring.io/guides/gs/messaging-redis/
- Baeldung – Spring Data Redis intro:
  https://www.baeldung.com/spring-data-redis-tutorial — most
  practical, examples for sets/values/expire.
- Redis SET commands (what opsForSet maps to — SADD, SREM, SCARD):
  https://redis.io/docs/latest/commands/?group=set
