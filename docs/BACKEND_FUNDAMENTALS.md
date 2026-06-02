# Backend Fundamentals — Deep Study Notes / أساسيات الـ Backend — ملاحظات معمّقة

> A deep, no-shortcuts guide to the core concepts every backend engineer must truly understand.
> Each concept is explained as: **what it is → the problem it solves → how it works → where it lives
> in THIS project → common mistakes.** Every English explanation line has an Arabic translation below it.
>
> <div dir="rtl">دليل معمّق وبدون اختصارات لأهم المفاهيم التي يجب على كل مهندس backend فهمها فهماً حقيقياً. كل مفهوم مشروح كالتالي: <strong>ما هو → المشكلة التي يحلها → كيف يعمل → أين يوجد في هذا المشروع → الأخطاء الشائعة.</strong> كل سطر شرح بالإنجليزية تحته ترجمته بالعربية.</div>

---

# 1. Transactions / المعاملات

### What it is / ما هو
A transaction is a group of database operations treated as one single, indivisible unit of work.
> <div dir="rtl">المعاملة هي مجموعة عمليات على قاعدة البيانات تُعامَل كوحدة عمل واحدة غير قابلة للتجزئة.</div>

### The problem it solves / المشكلة التي يحلها
Real actions need several steps. If the server crashes between step 1 and step 2, you are left with corrupt, half-finished data.
> <div dir="rtl">الأفعال الحقيقية تحتاج عدة خطوات. لو تعطّل الخادم بين الخطوة ١ والخطوة ٢، تبقى لديك بيانات فاسدة نصف مكتملة.</div>

Example: in a bank transfer you subtract from account A, then add to account B. A crash in the middle = money vanishes.
> <div dir="rtl">مثال: في التحويل البنكي تخصم من حساب A ثم تضيف إلى حساب B. التعطّل في المنتصف = اختفاء المال.</div>

### How it works / كيف يعمل
The database keeps a log. As you work, changes are tentative. `COMMIT` makes them permanent; `ROLLBACK` (or a crash) undoes them all.
> <div dir="rtl">تحتفظ قاعدة البيانات بسجل (log). أثناء العمل تكون التغييرات مؤقتة. أمر <code>COMMIT</code> يجعلها دائمة؛ و<code>ROLLBACK</code> (أو التعطّل) يتراجع عنها كلها.</div>

In Spring, `@Transactional` opens a transaction when the method starts and commits when it returns normally — or rolls back if it throws an exception.
> <div dir="rtl">في Spring، الـ <code>@Transactional</code> تفتح معاملة عند بداية الدالة وتعمل commit عند انتهائها بشكل طبيعي — أو rollback إذا رمت استثناءً.</div>

### In this project / في هذا المشروع
`AuthService.verifyPhone` ties "delete the verification code" and "mark the phone verified" into one unit.
> <div dir="rtl">دالة <code>AuthService.verifyPhone</code> تربط "حذف كود التحقق" و"تفعيل الرقم" في وحدة واحدة.</div>

```kotlin
@Transactional
fun verifyPhone(...) {
    phoneVerificationCodeRepository.delete(stored)        // step 1 / الخطوة ١
    userRepository.save(user.copy(phoneVerified = true))  // step 2 / الخطوة ٢
}
// If step 2 throws, step 1 is undone too. / لو رمت الخطوة ٢ استثناءً، يُلغى أثر الخطوة ١ أيضاً.
```

### Common mistakes / الأخطاء الشائعة
By default Spring only rolls back on **unchecked** exceptions (`RuntimeException`). A caught exception you swallow = no rollback.
> <div dir="rtl">افتراضياً يعمل Spring rollback فقط مع الاستثناءات <strong>غير المفحوصة</strong> (<code>RuntimeException</code>). الاستثناء الذي تلتقطه وتتجاهله = لا rollback.</div>

Calling a `@Transactional` method from *inside the same class* bypasses the proxy, so the transaction silently does not start.
> <div dir="rtl">استدعاء دالة <code>@Transactional</code> من *داخل نفس الكلاس* يتجاوز الـ proxy، فلا تبدأ المعاملة بصمت.</div>

> **Note (NoSQL):** MongoDB supports multi-document transactions only on a replica set. On a single
> node they may be no-ops — good to know for local dev vs production.
> <div dir="rtl"><strong>ملاحظة (NoSQL):</strong> MongoDB تدعم المعاملات متعددة المستندات فقط على replica set. على عقدة واحدة قد لا يكون لها أثر — من المفيد معرفة الفرق بين بيئة التطوير المحلية والإنتاج.</div>

---

# 2. ACID / خصائص ACID

ACID is the set of four guarantees that make a transaction trustworthy. Learn each letter deeply.
> <div dir="rtl">ACID هي مجموعة الضمانات الأربعة التي تجعل المعاملة جديرة بالثقة. افهم كل حرف بعمق.</div>

### A — Atomicity / الذرّية
All steps happen, or none do. There is no "half" state visible to anyone.
> <div dir="rtl">كل الخطوات تحدث، أو لا تحدث أي منها. لا توجد حالة "نصفية" يراها أحد.</div>

Mechanism: the database can undo (rollback) every change made so far if any step fails.
> <div dir="rtl">الآلية: تستطيع قاعدة البيانات التراجع عن كل تغيير تم حتى الآن إذا فشلت أي خطوة.</div>

### C — Consistency / الاتساق
A transaction moves the database from one valid state to another; all rules (constraints, types, foreign keys) stay satisfied.
> <div dir="rtl">تنقل المعاملة قاعدة البيانات من حالة صحيحة إلى أخرى صحيحة؛ وتبقى كل القواعد (القيود، الأنواع، المفاتيح) محقّقة.</div>

Example: a unique index on `phone` means a transaction can never commit two users with the same phone.
> <div dir="rtl">مثال: الفهرس الفريد على <code>phone</code> يعني أن المعاملة لا تستطيع أبداً حفظ مستخدمَين بنفس الرقم.</div>

### I — Isolation / العزل
Concurrent transactions must not see each other's unfinished work; each behaves as if it ran alone.
> <div dir="rtl">المعاملات المتزامنة يجب ألا ترى عمل بعضها غير المكتمل؛ كل واحدة تتصرف وكأنها تعمل وحدها.</div>

This is the hardest guarantee and the source of most concurrency bugs — covered in depth in section 3.
> <div dir="rtl">هذا أصعب ضمان ومصدر معظم مشاكل التزامن — مشروح بعمق في القسم ٣.</div>

### D — Durability / الديمومة
Once a transaction commits, its data survives crashes, restarts, and power loss (it is written to disk, not just memory).
> <div dir="rtl">بمجرد عمل commit للمعاملة، تبقى بياناتها رغم التعطّل وإعادة التشغيل وانقطاع الكهرباء (تُكتب على القرص وليس في الذاكرة فقط).</div>

Mechanism: the write-ahead log is flushed to durable storage before `COMMIT` returns success.
> <div dir="rtl">الآلية: يُكتب سجل الكتابة المسبقة (write-ahead log) إلى تخزين دائم قبل أن يُرجع الـ <code>COMMIT</code> نجاحاً.</div>

---

# 3. Isolation Levels & Concurrency Bugs / مستويات العزل ومشاكل التزامن

> This is the deep, interview-critical part of ACID. Most real bugs hide here.
> <div dir="rtl">هذا هو الجزء العميق والمهم في المقابلات من ACID. معظم المشاكل الحقيقية تختبئ هنا.</div>

When two transactions run at the same time, weaker isolation allows specific anomalies:
> <div dir="rtl">عندما تعمل معاملتان في نفس الوقت، يسمح العزل الأضعف بحدوث شذوذات محددة:</div>

**Dirty read (قراءة متّسخة):** you read data another transaction wrote but has not committed yet (it might roll back).
> <div dir="rtl">قراءة متّسخة: تقرأ بيانات كتبتها معاملة أخرى لكنها لم تعمل commit بعد (وقد تتراجع عنها).</div>

**Non-repeatable read (قراءة غير قابلة للتكرار):** you read a row twice in one transaction and get two different values because someone updated it in between.
> <div dir="rtl">قراءة غير قابلة للتكرار: تقرأ صفاً مرتين في نفس المعاملة فتحصل على قيمتين مختلفتين لأن أحداً عدّله بينهما.</div>

**Phantom read (قراءة شبحية):** you run the same query twice and new rows "appear" because another transaction inserted them.
> <div dir="rtl">قراءة شبحية: تنفّذ نفس الاستعلام مرتين فتظهر صفوف جديدة لأن معاملة أخرى أدرجتها.</div>

**Lost update (التحديث الضائع):** two transactions read the same value, both modify it, and the second overwrites the first's change.
> <div dir="rtl">التحديث الضائع: معاملتان تقرآن نفس القيمة، كلتاهما تعدّلها، فتطمس الثانية تغيير الأولى.</div>

### The four isolation levels (weak → strong) / مستويات العزل الأربعة (من الأضعف إلى الأقوى)
- **Read Uncommitted** — allows dirty reads. Almost never used. / يسمح بالقراءة المتّسخة. نادراً ما يُستخدم.
- **Read Committed** — no dirty reads (the common default). / لا قراءة متّسخة (الافتراضي الشائع).
- **Repeatable Read** — same row reads stay stable. / قراءات نفس الصف تبقى ثابتة.
- **Serializable** — fully isolated, as if transactions ran one-by-one (slowest, safest). / عزل كامل وكأن المعاملات تعمل واحدة تلو الأخرى (الأبطأ والأأمن).

### In this project — a real lost-update risk / في هذا المشروع — خطر حقيقي للتحديث الضائع
Your write methods read the whole user, copy it, and save the whole object back:
> <div dir="rtl">دوال الكتابة لديك تقرأ المستخدم كاملاً، تنسخه، ثم تحفظ الكائن كاملاً مرة أخرى:</div>

```kotlin
val user = userRepository.findById(id)          // reads followersCount = 100
userRepository.save(user.copy(name = newName))  // writes the WHOLE doc back
```

If someone follows you between the read and the save, `FollowService` does an atomic `+1` (→101) in Mongo, but your full-document save then overwrites it back to 100 — the new follower is lost.
> <div dir="rtl">لو تابعك أحد بين القراءة والحفظ، يقوم <code>FollowService</code> بزيادة ذرّية <code>+1</code> (←101) في Mongo، لكن حفظك للمستند كامل يطمسها ويعيدها إلى 100 — فيضيع المتابع الجديد.</div>

### The fixes / الحلول
**1. Targeted update** — update only the changed field, never the whole document:
> <div dir="rtl"><strong>١. تحديث موجّه</strong> — حدّث فقط الحقل المتغيّر، وليس المستند كاملاً:</div>
```kotlin
mongoTemplate.updateFirst(query(where("_id").isEqualTo(id)),
    Update().set("name", newName), User::class.java)   // touches name only / يلمس الاسم فقط
```

**2. Optimistic locking** — add a `@Version` field; the save fails if the document changed since you read it, and you retry.
> <div dir="rtl"><strong>٢. القفل التفاؤلي</strong> — أضف حقل <code>@Version</code>؛ يفشل الحفظ إذا تغيّر المستند منذ قراءتك له، فتعيد المحاولة.</div>

Optimistic vs pessimistic: optimistic assumes conflicts are rare and checks at write time; pessimistic locks the row up front so others wait.
> <div dir="rtl">التفاؤلي مقابل التشاؤمي: التفاؤلي يفترض أن التعارض نادر ويتحقق وقت الكتابة؛ التشاؤمي يقفل الصف مسبقاً فينتظر الآخرون.</div>

---

# 4. Indexing / الفهرسة

### What it is / ما هو
An index is a separate, sorted data structure (usually a B-tree) that maps a field's values to the location of matching documents.
> <div dir="rtl">الفهرس بنية بيانات منفصلة ومرتّبة (عادةً شجرة B-tree) تربط قيم حقل ما بمواقع المستندات المطابقة.</div>

### The problem it solves / المشكلة التي يحلها
Without an index, finding a row means reading every document — a "full collection scan", O(n). Slow and expensive as data grows.
> <div dir="rtl">بدون فهرس، إيجاد صف يعني قراءة كل المستندات — "مسح كامل للمجموعة"، بتعقيد O(n). بطيء ومكلف كلما زادت البيانات.</div>

### How it works / كيف يعمل
A B-tree keeps keys sorted, so the database can binary-search to the value in O(log n) instead of scanning everything.
> <div dir="rtl">شجرة B-tree تبقي المفاتيح مرتّبة، فتستطيع قاعدة البيانات البحث الثنائي عن القيمة بتعقيد O(log n) بدل المسح الكامل.</div>

Think of it like a book's index: instead of reading all pages to find a word, you jump straight to the listed page.
> <div dir="rtl">تخيّلها كفهرس كتاب: بدل قراءة كل الصفحات لإيجاد كلمة، تقفز مباشرة إلى الصفحة المذكورة.</div>

### Types you must know / أنواع يجب معرفتها
**Single-field index** — sorts one field. / فهرس حقل واحد — يرتّب حقلاً واحداً.

**Unique index** — also a constraint: forbids duplicate values (e.g. one phone per user). / فهرس فريد — قيد أيضاً: يمنع التكرار (رقم واحد لكل مستخدم).

**Compound index** — covers several fields together; order matters (equality field first, then the sort/range field). / فهرس مركّب — يغطّي عدة حقول معاً؛ الترتيب مهم (حقل المساواة أولاً ثم حقل الترتيب/المدى).

**TTL index** — auto-deletes documents after they expire (used for tokens/codes). / فهرس TTL — يحذف المستندات تلقائياً بعد انتهاء صلاحيتها (يُستخدم للتوكنات/الأكواد).

### In this project / في هذا المشروع
Every index is registered in `config/IndexInitializer.kt` (auto-index creation is turned off on purpose).
> <div dir="rtl">كل فهرس مسجّل في <code>config/IndexInitializer.kt</code> (إنشاء الفهارس التلقائي مُعطَّل عن قصد).</div>

```kotlin
ensureIndex("users", Index().on("phone", ASC).unique())            // fast login + uniqueness
ensureIndex("messages", Index().on("conversationId", ASC).on("createdAt", DESC)) // paged history
```
The messages index is compound: `conversationId` filters the chat, `createdAt DESC` returns newest-first — both served by one sorted read.
> <div dir="rtl">فهرس الرسائل مركّب: <code>conversationId</code> يصفّي المحادثة، و<code>createdAt DESC</code> يُرجع الأحدث أولاً — وكلاهما في قراءة مرتّبة واحدة.</div>

### Common mistakes / الأخطاء الشائعة
Indexing everything: each index slows every write (it must be updated too) and consumes disk/RAM. Index only what you actually query.
> <div dir="rtl">فهرسة كل شيء: كل فهرس يبطّئ كل عملية كتابة (لأنه يجب تحديثه أيضاً) ويستهلك القرص والذاكرة. افهرس فقط ما تستعلم عنه فعلاً.</div>

Wrong compound order: `(createdAt, conversationId)` would NOT serve "this chat, newest first" efficiently.
> <div dir="rtl">ترتيب مركّب خاطئ: <code>(createdAt, conversationId)</code> لن يخدم استعلام "هذه المحادثة، الأحدث أولاً" بكفاءة.</div>

---

# 5. SQL & JOINs / SQL وعمليات الربط

### What a JOIN is / ما هو الـ JOIN
A JOIN combines rows from two tables based on a related column (a key they share).
> <div dir="rtl">الـ JOIN يدمج صفوفاً من جدولين بناءً على عمود مرتبط (مفتاح مشترك بينهما).</div>

```sql
SELECT u.name, o.total
FROM users u
JOIN orders o ON o.user_id = u.id;   -- glue users to their orders / يلصق المستخدمين بطلباتهم
```

### The four JOIN types / أنواع الـ JOIN الأربعة
**INNER JOIN** — only rows that match in both tables. / فقط الصفوف المتطابقة في الجدولين.

**LEFT JOIN** — all rows from the left table + matches from the right (nulls where none). / كل صفوف الجدول الأيسر + المطابق من الأيمن (قيم فارغة عند عدم التطابق).

**RIGHT JOIN** — the mirror of LEFT. / عكس الـ LEFT.

**FULL JOIN** — all rows from both sides, matched where possible. / كل الصفوف من الجهتين، متطابقة حيثما أمكن.

### NoSQL contrast — important for you / المقارنة مع NoSQL — مهم لك
MongoDB (what this project uses) has no true JOINs. You either embed related data in one document, or fetch and stitch in application code.
> <div dir="rtl">MongoDB (التي يستخدمها هذا المشروع) لا تملك JOINs حقيقية. فإمّا أن تضمّن البيانات المرتبطة في مستند واحد، أو تجلبها وتدمجها في كود التطبيق.</div>

```kotlin
// ChatService — a manual JOIN: fetch users, then map them by id / ربط يدوي: نجلب المستخدمين ثم نفهرسهم بالـ id
val usersById = userRepository.findAllById(otherIds).associateBy { it.id }
```

Trade-off: embedding is fast to read but duplicates data; manual joins keep data clean but cost extra queries.
> <div dir="rtl">المقايضة: التضمين سريع في القراءة لكنه يكرّر البيانات؛ الربط اليدوي يبقي البيانات نظيفة لكنه يكلّف استعلامات إضافية.</div>

Still learn SQL JOINs — they are expected in most jobs and train how you reason about related data.
> <div dir="rtl">رغم ذلك تعلّم JOINs في SQL — فهي مطلوبة في معظم الوظائف وتدرّبك على التفكير في البيانات المترابطة.</div>

---

# 6. Distributed Systems / الأنظمة الموزّعة

### What it is / ما هو
A distributed system runs across multiple machines that cooperate over a network yet appear to users as a single system.
> <div dir="rtl">النظام الموزّع يعمل عبر عدة أجهزة تتعاون عبر الشبكة لكنه يظهر للمستخدمين كنظام واحد.</div>

### The core difficulty / الصعوبة الأساسية
The network is unreliable: messages can be lost, delayed, reordered, or duplicated, and any machine can die at any moment.
> <div dir="rtl">الشبكة غير موثوقة: الرسائل قد تضيع أو تتأخر أو يتغيّر ترتيبها أو تتكرر، وأي جهاز قد يتعطّل في أي لحظة.</div>

This single fact is why distributed systems are hard — you can never be 100% sure another node received your message.
> <div dir="rtl">هذه الحقيقة وحدها هي سبب صعوبة الأنظمة الموزّعة — لا يمكنك أبداً التأكد ١٠٠٪ أن العقدة الأخرى استلمت رسالتك.</div>

### When you become distributed / متى تصبح موزّعاً
The moment you run two or more instances of your app behind a load balancer, you are a distributed system.
> <div dir="rtl">في اللحظة التي تشغّل فيها نسختين أو أكثر من تطبيقك خلف موازِن أحمال، تصبح نظاماً موزّعاً.</div>

A trap appears immediately: per-instance memory state (a cache, a counter, a "who is online" map) is no longer shared — each replica has its own copy.
> <div dir="rtl">يظهر فخّ فوراً: الحالة المخزّنة في ذاكرة كل نسخة (كاش، عدّاد، خريطة "من المتصل") لم تعد مشتركة — لكل نسخة نسختها الخاصة.</div>

### In this project / في هذا المشروع
This app solved that by moving shared state out of memory and into **Redis**: presence, rate-limit buckets, and the token blacklist now live in one place every replica reads.
> <div dir="rtl">حلّ هذا التطبيق ذلك بنقل الحالة المشتركة من الذاكرة إلى <strong>Redis</strong>: الحضور، ودِلاء تحديد المعدل، والقائمة السوداء للتوكن أصبحت في مكان واحد تقرأه كل النسخ.</div>

It also uses Redis pub/sub so a WebSocket message received on instance A can be pushed to a user connected to instance B.
> <div dir="rtl">ويستخدم أيضاً Redis pub/sub حتى يمكن دفع رسالة WebSocket وصلت إلى النسخة A لمستخدم متصل بالنسخة B.</div>

---

# 7. CAP Theorem / نظرية CAP

### The statement / النص
During a network partition, a distributed system can guarantee at most two of: Consistency, Availability, Partition-tolerance.
> <div dir="rtl">أثناء انقسام الشبكة، يستطيع النظام الموزّع ضمان اثنتين على الأكثر من: الاتساق، التوفّر، تحمّل الانقسام.</div>

**C — Consistency:** every read returns the latest committed write (everyone sees the same data). / كل قراءة تُرجع آخر كتابة مؤكَّدة (الجميع يرى نفس البيانات).

**A — Availability:** every request receives a non-error response, even if not the freshest. / كل طلب يحصل على استجابة غير خاطئة، حتى لو لم تكن الأحدث.

**P — Partition tolerance:** the system keeps operating even when the network between nodes breaks. / يستمر النظام في العمل حتى عند انقطاع الشبكة بين العقد.

### Why it is really a choice between C and A / لماذا هو في الحقيقة اختيار بين C و A
Network partitions are a fact of life, so P is non-negotiable. When a partition happens you must choose: refuse to answer (keep C) or answer with possibly stale data (keep A).
> <div dir="rtl">انقسام الشبكة أمر واقع، لذا P غير قابل للتفاوض. وعند حدوث انقسام يجب أن تختار: ترفض الرد (تحافظ على C) أو ترد ببيانات قد تكون قديمة (تحافظ على A).</div>

**CP systems** (banking, inventory) prefer correctness: better to fail than show wrong data. / **أنظمة CP** (البنوك، المخزون) تفضّل الصحة: الفشل أفضل من عرض بيانات خاطئة.

**AP systems** (presence, feeds, likes) prefer availability: better to show slightly old data than to be down. / **أنظمة AP** (الحضور، الخلاصات، الإعجابات) تفضّل التوفّر: عرض بيانات قديمة قليلاً أفضل من التوقف.

### In this project / في هذا المشروع
Presence is intentionally AP: if a user shows "online" a few seconds after they left, nothing breaks. Availability matters more than perfect accuracy here.
> <div dir="rtl">الحضور AP عن قصد: لو ظهر مستخدم "متصل" بعد مغادرته ببضع ثوانٍ، لا يتعطّل شيء. التوفّر هنا أهم من الدقة المثالية.</div>

> **Bonus — PACELC:** extends CAP — *if* Partitioned choose A or C; *Else* (normal operation) choose
> Latency or Consistency. Real systems trade latency for consistency even when healthy.
> <div dir="rtl"><strong>إضافة — PACELC:</strong> تمتد على CAP — *إذا* حدث انقسام اختر A أو C؛ *وإلا* (التشغيل العادي) اختر زمن الاستجابة أو الاتساق. الأنظمة الحقيقية تقايض زمن الاستجابة بالاتساق حتى في الحالة السليمة.</div>

---

# 8. Authentication vs Authorization / المصادقة مقابل التفويض

> These are two different questions, often answered in two different layers. Confusing them is a classic mistake.
> <div dir="rtl">هذان سؤالان مختلفان، يُجاب عنهما غالباً في طبقتين مختلفتين. الخلط بينهما خطأ كلاسيكي.</div>

### Authentication = "Who are you?" / المصادقة = "من أنت؟"
Proving identity. In this project the JWT proves it: a signed token the server can trust without a database lookup.
> <div dir="rtl">إثبات الهوية. في هذا المشروع يثبتها الـ JWT: توكن موقّع يستطيع الخادم الوثوق به دون الرجوع إلى قاعدة البيانات.</div>

`JwtAuthFilter` runs on every request and does authentication in this exact order:
> <div dir="rtl">الـ <code>JwtAuthFilter</code> يعمل على كل طلب ويقوم بالمصادقة بهذا الترتيب بالضبط:</div>

1. Read the `Authorization: Bearer <token>` header. / اقرأ ترويسة `Authorization: Bearer <token>`.
2. Validate the signature and that it is an access token (not forged, not a refresh token). / تحقّق من التوقيع وأنه access token (غير مزوّر، وليس refresh).
3. Check the token is not revoked (the Redis blacklist) — even a valid token can be killed on logout. / تحقّق أن التوكن غير ملغى (القائمة السوداء في Redis) — حتى التوكن الصحيح يمكن إبطاله عند الخروج.
4. Build an `Authentication` (userId + roles from the claims) and store it in the `SecurityContext`. / ابنِ `Authentication` (المعرّف + الأدوار من الـ claims) واحفظها في `SecurityContext`.

The order matters: validate first, then check revocation. A forged token must never reach the blacklist check.
> <div dir="rtl">الترتيب مهم: تحقّق أولاً ثم افحص الإلغاء. التوكن المزوّر يجب ألا يصل أبداً إلى فحص القائمة السوداء.</div>

Why read roles from the token, not the DB? It keeps auth **stateless** and fast (no DB hit per request). The trade-off: a role change only takes effect when the token expires — which is why the blacklist exists for forced logout.
> <div dir="rtl">لماذا نقرأ الأدوار من التوكن وليس من قاعدة البيانات؟ ليبقى التحقق <strong>بلا حالة</strong> وسريعاً (دون استعلام لكل طلب). المقايضة: تغيير الدور لا يسري إلا عند انتهاء التوكن — ولهذا توجد القائمة السوداء للخروج القسري.</div>

### Authorization = "Are you allowed to do THIS?" / التفويض = "هل يُسمح لك بفعل هذا؟"
Checking permission for a specific action on a specific resource. It happens AFTER authentication.
> <div dir="rtl">فحص الإذن لفعل محدد على مورد محدد. يحدث بعد المصادقة.</div>

There are two flavors, and this project uses both:
> <div dir="rtl">هناك نوعان، وهذا المشروع يستخدم كليهما:</div>

**Role-based (coarse):** "only ADMIN may call `/admin/**`." Answered from the roles in the JWT.
> <div dir="rtl"><strong>قائم على الأدوار (خشن):</strong> "فقط ADMIN يستدعي <code>/admin/**</code>". يُجاب عنه من الأدوار في الـ JWT.</div>

**Ownership-based (fine-grained):** "is this user a participant in THIS conversation?" Roles cannot answer this — it needs the actual data, so it lives in the service.
> <div dir="rtl"><strong>قائم على الملكية (دقيق):</strong> "هل هذا المستخدم مشارك في هذه المحادثة؟" الأدوار لا تجيب — يحتاج البيانات الفعلية، لذا يكون في الـ service.</div>

```kotlin
if (userId !in conversation.participantIds) {
    // 404 (not 403): hide the resource's existence from non-participants, preventing enumeration.
    // 404 (وليس 403): نخفي وجود المورد عن غير المشاركين، فنمنع هجمات التعداد.
    throw ApiException.NotFound("error.chat.conversation_not_found")
}
```

Fine-grained authorization grows only when features demand it (blocking, privacy, sharing). You add a check next to the data, not a big framework.
> <div dir="rtl">التفويض الدقيق ينمو فقط عندما تتطلبه الميزات (الحظر، الخصوصية، المشاركة). تضيف فحصاً بجوار البيانات، وليس إطاراً ضخماً.</div>

---

# 9. Security & OWASP / الأمان و OWASP

OWASP publishes the "Top 10": the ten most common web vulnerabilities. The golden rule: never trust input, never trust the client.
> <div dir="rtl">OWASP ينشر قائمة "أهم ١٠": أكثر عشر ثغرات شيوعاً في الويب. القاعدة الذهبية: لا تثق بالمدخلات أبداً، ولا تثق بالعميل أبداً.</div>

**① Broken Access Control** — #1 risk: reaching data that isn't yours (IDOR: changing `/chats/123` to `/chats/124`). Defense: check ownership, as this project does with `participantIds` + a 404 response.
> <div dir="rtl"><strong>التحكم في الوصول المكسور</strong> — الخطر رقم ١: الوصول لبيانات ليست لك (IDOR: تغيير <code>/chats/123</code> إلى <code>/chats/124</code>). الدفاع: فحص الملكية، كما يفعل المشروع عبر <code>participantIds</code> ورد 404.</div>

**② Injection** — input treated as code/query (SQL injection, malicious regex). Defense: parameterized queries + escaping, e.g. `Regex.escape(query)` in user search.
> <div dir="rtl"><strong>الحقن</strong> — معاملة المدخلات كأنها كود/استعلام (حقن SQL، regex خبيث). الدفاع: استعلامات بمعاملات + تعقيم، مثل <code>Regex.escape(query)</code> في البحث.</div>

**③ Cryptographic Failures** — storing secrets badly. Defense: BCrypt for passwords, SHA-256 for codes, JWT secret in an env var — all done here.
> <div dir="rtl"><strong>فشل التشفير</strong> — تخزين الأسرار بشكل سيئ. الدفاع: BCrypt لكلمات المرور، SHA-256 للأكواد، سر الـ JWT في متغير بيئة — وكلها مطبّقة هنا.</div>

**④ Authentication Failures** — weak login/session handling, brute force. Defense: access/refresh JWTs, token blacklist, rate limiting on login.
> <div dir="rtl"><strong>فشل المصادقة</strong> — ضعف الدخول/الجلسات، التخمين. الدفاع: توكنات access/refresh، قائمة سوداء، تحديد معدل للدخول.</div>

**Others to know:** Security Misconfiguration (exposed `/actuator`), Vulnerable Dependencies (old libraries), Insecure Design, Logging/Monitoring failures, SSRF.
> <div dir="rtl"><strong>أمور أخرى:</strong> سوء إعداد الأمان (كشف <code>/actuator</code>)، اعتماديات بها ثغرات، تصميم غير آمن، ضعف السجلات والمراقبة، SSRF.</div>

---

# 10. Idempotency & Retries / العمليات العديمة الأثر وإعادة المحاولة

### What idempotency means / ما معنى idempotency
An operation is idempotent if performing it many times has the same effect as performing it once.
> <div dir="rtl">العملية عديمة الأثر إذا كان تنفيذها مرات عديدة يعطي نفس نتيجة تنفيذها مرة واحدة.</div>

### Why it matters / لماذا يهم
Networks time out, so clients retry. But the first request may have already succeeded — only the response was lost — so the server runs it twice.
> <div dir="rtl">الشبكات تنتهي مهلتها فيعيد العميل المحاولة. لكن الطلب الأول قد يكون نجح فعلاً — وضاعت الاستجابة فقط — فينفّذه الخادم مرتين.</div>

### Safe vs unsafe to retry / الآمن وغير الآمن للإعادة
Naturally idempotent: GET (reads nothing), PUT (sets a value), DELETE (already-deleted stays deleted).
> <div dir="rtl">عديمة الأثر بطبيعتها: GET (لا تغيّر)، PUT (تعيين قيمة)، DELETE (المحذوف يبقى محذوفاً).</div>

Dangerous: POST that creates a row, or `count += 1` — running twice creates a duplicate or double-counts.
> <div dir="rtl">خطِرة: POST الذي ينشئ سجلاً، أو <code>count += 1</code> — التنفيذ مرتين يُنشئ تكراراً أو يحسب مرتين.</div>

### The idempotency key / مفتاح idempotency
The client sends a unique id per action; the server records it (often in Redis with a TTL) and, if the same key returns, replies with the original result instead of re-running.
> <div dir="rtl">يرسل العميل معرّفاً فريداً لكل فعل؛ ويسجّله الخادم (غالباً في Redis مع مدة صلاحية)، وإذا عاد نفس المفتاح يردّ بالنتيجة الأصلية بدل إعادة التنفيذ.</div>

### In this project / في هذا المشروع
`conversationRepository.findOrCreate` is idempotent by design: opening the same chat twice never creates a duplicate, enforced by the unique `pairKey` index.
> <div dir="rtl">دالة <code>conversationRepository.findOrCreate</code> عديمة الأثر بالتصميم: فتح نفس المحادثة مرتين لا يُنشئ نسخة مكررة، بفضل فهرس <code>pairKey</code> الفريد.</div>

The rule: design operations to be idempotent so retries are always safe — because in distributed systems, retries are guaranteed to happen.
> <div dir="rtl">القاعدة: صمّم العمليات لتكون عديمة الأثر لتكون إعادة المحاولة آمنة دائماً — لأن إعادة المحاولة في الأنظمة الموزّعة مضمونة الحدوث.</div>

---

# 11. Architecture Patterns / أنماط المعمارية

### Modular Monolith / المونوليث المعياري ← *this project / هذا المشروع*
One deployable application, internally split into clean, independent modules (`auth/`, `user/`, `chat/`, `follows/`), each owning its own logic and data access.
> <div dir="rtl">تطبيق واحد قابل للنشر، مقسّم داخلياً إلى وحدات نظيفة ومستقلة (<code>auth/</code>، <code>user/</code>، <code>chat/</code>، <code>follows/</code>)، كل وحدة تملك منطقها ووصولها للبيانات.</div>

Strengths: one codebase, one deploy, easy debugging, real transactions across modules, low operational cost.
> <div dir="rtl">نقاط القوة: قاعدة كود واحدة، نشر واحد، تصحيح سهل، معاملات حقيقية عبر الوحدات، وتكلفة تشغيل منخفضة.</div>

Because the modules are clean, any one can later be extracted into its own service if a real need appears. This is the right default for most apps.
> <div dir="rtl">ولأن الوحدات نظيفة، يمكن لاحقاً فصل أي منها إلى خدمة مستقلة عند ظهور حاجة حقيقية. وهذا الخيار الافتراضي الصحيح لمعظم التطبيقات.</div>

### Microservices / الخدمات المصغّرة
Each capability becomes its own independently deployable service, with its own database, communicating over HTTP or messaging.
> <div dir="rtl">كل قدرة تصبح خدمة مستقلة قابلة للنشر بقاعدة بياناتها الخاصة، وتتواصل عبر HTTP أو الرسائل.</div>

Pros: independent scaling and deployment, clear team ownership, fault isolation.
> <div dir="rtl">المزايا: توسّع ونشر مستقل، ملكية واضحة للفرق، وعزل الأعطال.</div>

Cons: network calls everywhere (slower, can fail), distributed transactions, complex deployment/monitoring, much harder debugging.
> <div dir="rtl">العيوب: استدعاءات شبكية في كل مكان (أبطأ وقد تفشل)، معاملات موزّعة، نشر ومراقبة معقّدة، وتصحيح أصعب بكثير.</div>

Rule of thumb: microservices solve an organizational problem (many teams stepping on each other), not a performance problem. Don't start here.
> <div dir="rtl">القاعدة العامة: الخدمات المصغّرة تحل مشكلة تنظيمية (فرق كثيرة تتعارض)، وليست مشكلة أداء. لا تبدأ من هنا.</div>

### Event-Driven Architecture / المعمارية المدفوعة بالأحداث
Instead of component A calling B directly, A emits an event ("MessageSent") and any interested component reacts. Producers and consumers don't know about each other.
> <div dir="rtl">بدل أن يستدعي المكوّن A الـ B مباشرة، يُطلق A حدثاً ("MessageSent") ويتفاعل معه أي مكوّن مهتم. المُنتِج والمستهلك لا يعرف أحدهما الآخر.</div>

Benefits: loose coupling, easy to add new reactions later, natural fit for async work and scaling.
> <div dir="rtl">الفوائد: ترابط ضعيف، سهولة إضافة تفاعلات جديدة لاحقاً، ومناسبة طبيعية للعمل غير المتزامن والتوسّع.</div>

This project already has a seed of it: Redis pub/sub fans live chat messages across instances with no direct instance-to-instance calls. At larger scale you'd use Kafka or RabbitMQ.
> <div dir="rtl">هذا المشروع يملك بذرة منها بالفعل: Redis pub/sub يوزّع رسائل الشات الحيّة عبر النسخ دون استدعاءات مباشرة بين النسخ. وعلى نطاق أكبر تستخدم Kafka أو RabbitMQ.</div>

---

# The Big Picture / الصورة الكاملة

```
Modular monolith (one app, clean modules) / مونوليث معياري (تطبيق واحد، وحدات نظيفة)
   │
   ├─ Request arrives / يصل الطلب
   │    └─ JwtAuthFilter  → Authentication "who are you?" (validate + blacklist)
   │    └─ Service layer  → Authorization "are you allowed?" (participantIds)
   │
   ├─ MongoDB → transactions + ACID + indexes (no JOINs → stitch in code)
   │             └─ watch isolation: lost-update on full-document saves
   │
   └─ Redis   → makes it a distributed system / يجعله نظاماً موزّعاً
                  ├─ shared state (presence, rate-limit, token blacklist) → CAP: chose AP
                  ├─ idempotency keys (when needed) / مفاتيح idempotency عند الحاجة
                  └─ pub/sub bridge → event-driven seed / بذرة المعمارية المدفوعة بالأحداث
```

---

# Is this enough? / هل هذا كافٍ؟

These 11 topics are a strong, job-ready foundation and cover most "fundamentals" interviews. They are not the finish line.
> <div dir="rtl">هذه المواضيع الـ ١١ أساس قوي وجاهز للعمل وتغطي معظم مقابلات "الأساسيات". لكنها ليست خط النهاية.</div>

### Already strong in this project / مواضيع قوية بالفعل في هذا المشروع
- Authentication & JWT (access/refresh, blacklist). / المصادقة و JWT.
- Authorization (role-based + ownership checks). / التفويض (الأدوار + فحص الملكية).
- Rate limiting (bucket4j on Redis). / تحديد المعدل.
- Real-time WebSockets (STOMP, presence). / الزمن الحقيقي عبر WebSockets.
- Distributed shared state via Redis. / الحالة المشتركة الموزّعة عبر Redis.

### Learn next (high priority) / تعلّمها تالياً (أولوية عالية)
- Concurrency & locking (optimistic vs pessimistic) — directly relevant to your lost-update risk. / التزامن والأقفال.
- Caching strategies (cache-aside, write-through, invalidation, TTL). / استراتيجيات الكاش.
- HTTP/REST in depth (status codes, idempotency, pagination, versioning). / HTTP/REST بعمق.
- Database scaling (replication, read replicas, sharding, connection pools). / توسيع قواعد البيانات.
- Message queues (Kafka/RabbitMQ, at-least-once vs exactly-once). / طوابير الرسائل.

### Learn after (medium priority) / تعلّمها لاحقاً (أولوية متوسطة)
- Observability (logging, metrics, tracing). / المراقبة.
- Testing (unit, integration, testcontainers). / الاختبارات.
- Eventual consistency & the SAGA pattern. / الاتساق النهائي ونمط SAGA.
- Load balancing & API gateways. / موازنة الأحمال وبوابات الـ API.
- Containerization & CI/CD. / الحاويات و CI/CD.

### Suggested learning order / الترتيب المقترح للتعلّم
1. Transactions + ACID + Isolation / المعاملات + ACID + العزل
2. Indexing / الفهرسة
3. SQL + JOINs / SQL و JOINs
4. Concurrency & locking / التزامن والأقفال
5. Authentication & Authorization / المصادقة والتفويض
6. Security & OWASP / الأمان و OWASP
7. CAP + distributed basics / CAP وأساسيات التوزيع
8. Caching + idempotency + message queues / الكاش و idempotency وطوابير الرسائل
9. Monolith vs microservices vs event-driven / المونوليث مقابل الخدمات المصغّرة مقابل الأحداث

> The deepest learning comes from building, breaking, and fixing real systems — exactly what you are
> doing with this project. Read this doc, then go change the code and watch what happens.
> <div dir="rtl">أعمق تعلّم يأتي من بناء الأنظمة الحقيقية وكسرها وإصلاحها — وهو بالضبط ما تفعله في هذا المشروع. اقرأ هذا الملف، ثم اذهب وغيّر الكود وراقب ما يحدث.</div>
