# Future Features — "Nice to Have, Not Now"

Things that are valuable in production but overkill for the current single-service, no-traffic stage. Documented here so the reasoning is captured and they can be picked up later without re-researching.

---

## Prometheus metrics + Grafana dashboards

- **Prometheus** — a time-series database that scrapes numbers from your app every ~15s (requests/sec, response time, memory, error count) and stores them.
- **Grafana** — the dashboard UI that draws charts from those numbers.
- **Together** — a car dashboard for your app: speed, fuel, engine temp, in real time.

Spring Boot Actuator (already added) exposes `/actuator/prometheus`; Prometheus just scrapes that endpoint.

**Why it matters:** "the app crashed at 3am — why?" becomes answerable.

**Not now:** no production users to monitor.

> Already wired up on the Spring side (`micrometer-registry-prometheus` + actuator exposure + security whitelist). Docker Compose for Prometheus + Grafana lives under `monitoring/` and is ready to `docker compose up` whenever needed.

---

## Distributed tracing (OpenTelemetry)

In a microservices system, one user request hops **Service A → B → C → DB**. If it's slow, which hop is slow?

Tracing tags every incoming request with a unique ID, follows it across services, and gives a timeline view:

> A took 5ms, B took 200ms, DB took 12ms.

**OpenTelemetry (OTel)** is the standard library/protocol for emitting traces. Add the OTel Java agent → Spring MVC, JDBC, HTTP clients, Mongo are all auto-instrumented with zero code changes. Spans get shipped to a backend (Jaeger, Tempo, Datadog) that stitches them into waterfall views.

**Why it matters:** debugging latency in multi-service architectures.

**Not now:** single service — there's no "where's the time going" mystery to solve.

---

## Kubernetes manifests

**Kubernetes (K8s)** is an orchestrator that runs Docker containers across many machines:

- Auto-restart on crashes
- Route traffic between copies
- Zero-downtime rolling deploys
- Scale up under load

**Manifests** are YAML files describing the desired state:

> "3 copies of this container, port 8181, 512MB RAM, restart if it dies."

You hand them to K8s (`kubectl apply -f .`) and it makes reality match the file.

**Why it matters:** serious production, multiple servers, uptime is non-negotiable.

**Not now:** a single container on Render / Railway / Fly.io is simpler and sufficient. K8s can be learned locally for free via Docker Desktop, `minikube`, or `kind` — useful for a CV, not needed for shipping.

---

## Roadmap order (when revisiting)

1. **Deploy somewhere** (Render / Fly.io with `./gradlew bootBuildImage`) — gives real traffic to observe.
2. **Prometheus + Grafana** — once there's traffic, metrics actually mean something.
3. **OpenTelemetry** — once there's a second service or external API call worth tracing.
4. **Kubernetes** — only after outgrowing one-click platforms, or when joining a team that already runs on K8s.
