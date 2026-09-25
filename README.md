# SYNTRA

Self-hosted internal mail and chat. Admins create every account. Members get private mail, direct chats, and group rooms. A Python agent drafts, summarizes, and answers from room history. Prometheus and Grafana watch delivery.

## Run

```bash
docker compose up --build
```

Open http://localhost

| Account | Password | Role |
|---|---|---|
| admin | Admin@123 | Admin |
| aisha | User@123 | User |
| kabir | User@123 | User |

Grafana is at http://localhost:3001 (`admin` / `admin`). Prometheus is at http://localhost:9090.

## What it includes

- JWT login and admin-only onboarding
- Inbox, sent, starred, archive, custom folders, search, reply, forward, attachments
- Direct messages and group rooms over STOMP/SockJS, with typing, presence, and unread counts
- Kafka topic `syntra.chat` for live fan-out, Redis for presence and unread counts
- FastAPI agent: priority, draft, thread summary, room Q&A. Set `LLM_API_KEY` and `LLM_BASE_URL` for a real model; otherwise it uses local rules
- Admin health, metric snapshots, and incidents when message rate drops or logins fail
- JUnit tests for message rules, priority, and mail access

API and realtime run in one Spring Boot process. Chat is published to Kafka and a consumer broadcasts it, so a second instance can join the same flow.

## Local development

Start MySQL, Redis, and Kafka, then:

```bash
cd backend && ./mvnw spring-boot:run
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --port 8000
cd front && npm install && npm run dev
```

The Vite app is at http://localhost:5173 and proxies `/api` and `/ws` to port 8080.

Database, Redis, Kafka, JWT, and the agent URL come from environment variables. See `backend/src/main/resources/application.yml`.
