# Tech Stack - LegalConnect

## Backend

| Category | Technology | Version / Notes |
|---|---|---|
| **Language** | Java | 17+ |
| **Framework** | Spring Boot | 3.x |
| **Security** | Spring Security | JWT-based auth + role-based access |
| **Token Storage** | JWT (JSON Web Tokens) | Access token (15 min) + Refresh token (7 days) |
| **ORM** | Spring Data JPA / Hibernate | Repository pattern |
| **Database** | MySQL | Compatible with PostgreSQL |
| **Real-time** | Spring WebSockets | STOMP protocol over SockJS |
| **HTTP Client** | OkHttp | For calling OpenAI APIs |
| **JSON** | Jackson (FasterXML) | Request/response serialization |
| **Payment** | Razorpay Java SDK | Order creation + webhook verification |
| **Build Tool** | Maven | `mvn spring-boot:run` |
| **Logging** | SLF4J + Logback | Structured logging across all services |

---

## Frontend

| Category | Technology | Notes |
|---|---|---|
| **Framework** | React.js | 18+ |
| **State Management** | React Context API + Hooks | Auth state, user/role session |
| **HTTP Client** | Axios | Auto-attaches JWT via interceptors |
| **Real-time Chat** | SockJS + StompJS | WebSocket fallback for browsers |
| **Styling** | Vanilla CSS | CSS Variables, Glassmorphism, gradients |
| **Icons** | React Icons | Lucide + FontAwesome |
| **Build Tool** | npm | `npm start` for dev server |

---

## AI / Machine Learning

| Model / API | Provider | Purpose |
|---|---|---|
| **Whisper** (`whisper-1`) | OpenAI | Audio → English text transcription |
| **GPT-4o** | OpenAI | Case classification, PII masking, title generation |
| **GPT-4o** | OpenAI | English → Gujarati translation |
| **GPT-4o-mini** | OpenAI | Speaker gender detection |
| **gpt-4o-mini-tts** | OpenAI | On-demand Text-to-Speech (MP3) |

### TTS Voices Used

| Language | Gender | Voice |
|---|---|---|
| English | MALE | `cedar` |
| English | FEMALE | `coral` |
| English | NEUTRAL | `ash` |
| Gujarati | MALE | `onyx` |
| Gujarati | FEMALE | `coral` |
| Gujarati | NEUTRAL | `nova` |

---

## Infrastructure & DevOps

| Component | Technology | Notes |
|---|---|---|
| **Database** | MySQL Server | Local / cloud-hosted |
| **App Server** | Embedded Tomcat | Bundled in Spring Boot JAR |
| **Environment Config** | `application.properties` | API keys, DB URL, Razorpay keys |
| **Prerequisites** | JDK 17+, Node.js 18+, Maven 3.8+, MySQL | Required for local dev |

---

## Security Stack

| Mechanism | Implementation |
|---|---|
| Authentication | JWT Bearer Token in `Authorization` header |
| Refresh Tokens | DB-backed `RefreshToken` entity |
| Role-Based Access | `AuthorizationService` enforces USER / LAWYER / ADMIN |
| PII Protection | AI-driven masking before any case data is shared |
| Payment Security | Razorpay HMAC-SHA256 webhook signature verification |
| Idempotency | Unique keys on `payments` table to prevent double charge |
| Rate Limiting | `RateLimitService` for abuse prevention |
| Soft Delete | `deleted` flag on `cases` — no permanent deletion |

---

## Key Libraries & Dependencies

| Library | Purpose |
|---|---|
| `spring-boot-starter-security` | Security filter chain & authorization |
| `spring-boot-starter-websocket` | WebSocket + STOMP broker |
| `spring-boot-starter-data-jpa` | Repository layer |
| `com.razorpay:razorpay-java` | Razorpay payment gateway SDK |
| `okhttp3` | Async HTTP client for OpenAI API calls |
| `jackson-databind` | JSON serialization / deserialization |
| `io.jsonwebtoken:jjwt` | JWT generation and validation |
| `sockjs-client` (npm) | WebSocket polyfill for browsers |
| `@stomp/stompjs` (npm) | STOMP messaging client |
| `axios` (npm) | HTTP client with interceptors |
| `react-icons` (npm) | Icon library |
