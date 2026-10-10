<div align="center">

# 🐛 TrackForge

### Industry-Grade Bug Tracking System

A full-stack bug tracking platform built with **microservices architecture**, **event-driven design**, **real-time updates**, and **AI-powered triage**.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue?style=flat-square&logo=typescript)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.6-black?style=flat-square&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-black?style=flat-square&logo=githubactions)

</div>

---

## 📌 Overview

TrackForge is a production-ready bug tracking system designed to demonstrate industry-level software engineering practices. Built as a portfolio project showcasing microservices, event-driven architecture, real-time features, and modern DevOps.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    React + TypeScript                        │
│           (Vite · TanStack Query · dnd-kit · Recharts)      │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│              API Gateway  (Spring Cloud Gateway)             │
│              JWT validation · CORS · Routing                 │
└───┬──────────────┬───────────────┬────────────────┬─────────┘
    │              │               │                │
┌───▼───┐    ┌────▼────┐   ┌──────▼──────┐  ┌─────▼──────┐
│ Auth  │    │   Bug   │   │Notification │  │ Analytics  │
│Service│    │ Service │   │  Service    │  │  Service   │
│ :8081 │    │  :8082  │   │   :8083     │  │   :8084    │
└───┬───┘    └────┬────┘   └──────┬──────┘  └─────┬──────┘
    │              │               │                │
    └──────────────┴───────────────┴────────────────┘
                           │
              ┌────────────▼────────────┐
              │      Apache Kafka        │
              │  user-events             │
              │  bug-events              │
              │  comment-events          │
              └────────────┬────────────┘
                           │
    ┌──────────────┬────────┴──────┬────────────┐
    │              │               │            │
┌───▼───┐    ┌────▼────┐   ┌──────▼──┐  ┌─────▼──────┐
│Postgres│   │  Redis  │   │  MinIO  │  │ Prometheus │
│pgvector│   │  Cache  │   │   S3    │  │ + Grafana  │
└────────┘   └─────────┘   └─────────┘  └────────────┘
```

---

## ✨ Features

### Core
- 🔐 **JWT Authentication** — Register, login, refresh tokens, role-based access (Admin, PM, Developer, Tester)
- 📁 **Project Management** — Create projects with unique keys (e.g. SEP-1, APP-42)
- 🐛 **Bug Tracking** — Full lifecycle: Open → In Progress → In Review → Resolved → Closed
- 💬 **Comments** — Threaded discussion on every bug with delete own comment
- 📎 **File Attachments** — Upload screenshots and logs to bugs via MinIO (S3-compatible)
- 🔔 **Email Notifications** — Bug assignment and status change emails via SendGrid
- 📊 **Analytics Dashboard** — Bug trends, priority breakdown, resolution charts

### Advanced
- ⚡ **Real-time Updates** — WebSocket (STOMP + SockJS) pushes live bug changes to all viewers
- 🎯 **Kanban Board** — Drag-and-drop bug cards across status columns (dnd-kit)
- 🔍 **Full-text Search** — PostgreSQL-powered search across bug titles and descriptions
- 🤖 **AI Bug Triage** — Auto-suggests priority and severity using OpenAI API
- 🧠 **Duplicate Detection** — pgvector cosine similarity to find semantically similar bugs
- 📝 **Audit Log** — Every status, priority, and assignee change is recorded per bug
- 📦 **Redis Caching** — 5-minute TTL cache on bug lists and stats

### DevOps
- 🐳 **Docker Compose** — Full local stack in one command
- ⚙️ **GitHub Actions CI/CD** — Test → Build → Push Docker images to GHCR on every push
- 📈 **Prometheus + Grafana** — Metrics from all 4 Spring Boot services
- 🔧 **Swagger UI** — Auto-generated API docs on every service

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| **API Gateway** | Spring Cloud Gateway |
| **Messaging** | Apache Kafka |
| **Database** | PostgreSQL 16 + pgvector |
| **Cache** | Redis 7 |
| **File Storage** | MinIO (S3-compatible) |
| **Email** | SendGrid |
| **AI** | OpenAI API (gpt-4o-mini + text-embedding-3-small) |
| **Frontend** | React 18, TypeScript, Vite, TailwindCSS |
| **State** | TanStack Query v5, Zustand |
| **Real-time** | STOMP WebSocket, SockJS |
| **UI** | dnd-kit (Kanban), Recharts (Analytics), Lucide Icons |
| **DevOps** | Docker Compose, GitHub Actions, Prometheus, Grafana |

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop
- Java 17+
- Node.js 20+
- Maven 3.9+

### 1. Clone the repo
```bash
git clone https://github.com/YOUR_USERNAME/TrackForge.git
cd TrackForge
```

### 2. Set up environment
```bash
cp .env.example .env
# Edit .env if needed — defaults work for local development
```

### 3. Start infrastructure
```bash
docker compose up -d postgres redis zookeeper kafka minio kafka-ui prometheus grafana
```

### 4. Create databases (first time only)
```bash
docker exec bt-postgres psql -U postgres -c "CREATE DATABASE trackforge_auth;"
docker exec bt-postgres psql -U postgres -c "CREATE DATABASE trackforge_bugs;"
docker exec bt-postgres psql -U postgres -c "CREATE DATABASE trackforge_notifications;"
docker exec bt-postgres psql -U postgres -c "CREATE DATABASE trackforge_analytics;"
```

### 5. Run backend services (each in a separate terminal)
```bash
# Terminal 1
cd services/auth-service && mvn clean spring-boot:run

# Terminal 2
cd services/bug-service && mvn clean spring-boot:run

# Terminal 3
cd services/notification-service && mvn clean spring-boot:run

# Terminal 4
cd services/api-gateway && mvn clean spring-boot:run
```

### 6. Run frontend
```bash
cd frontend
npm install
npm run dev
```

### 7. Open the app
```
http://localhost:5173
```

---

## 🌐 Service URLs

| Service | URL |
|---|---|
| **Frontend** | http://localhost:5173 |
| **API Gateway** | http://localhost:8080 |
| **Auth Service + Swagger** | http://localhost:8081/swagger-ui.html |
| **Bug Service + Swagger** | http://localhost:8082/swagger-ui.html |
| **Notification Service** | http://localhost:8083/swagger-ui.html |
| **Kafka UI** | http://localhost:9090 |
| **MinIO Console** | http://localhost:9001 (minioadmin / minioadmin) |
| **Grafana** | http://localhost:3001 (admin / admin) |
| **Prometheus** | http://localhost:9091 |

---

## 📂 Project Structure

```
TrackForge/
├── .github/
│   └── workflows/
│       └── ci-cd.yml              # GitHub Actions pipeline
├── services/
│   ├── api-gateway/               # Spring Cloud Gateway + JWT filter
│   ├── auth-service/              # Register · Login · Refresh · JWT
│   ├── bug-service/               # Bugs · Projects · Comments · WS · AI
│   └── notification-service/      # Kafka consumer → SendGrid emails
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── ui/                # Reusable UI components
│       │   ├── layout/            # Sidebar, navigation
│       │   └── kanban/            # Drag-and-drop Kanban board
│       ├── pages/                 # Route-level pages
│       ├── hooks/                 # useWebSocket, custom hooks
│       ├── services/              # Axios API client
│       ├── store/                 # Zustand auth store
│       ├── types/                 # TypeScript types
│       └── utils/                 # Helpers, color maps
├── infrastructure/
│   ├── k8s/                       # Kubernetes manifests (future)
│   └── prometheus/                # Prometheus scrape config
├── scripts/
│   └── init-db.sql                # Database initialization
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🔌 API Endpoints

### Auth Service (port 8081)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register new user | Public |
| POST | `/api/auth/login` | Login, returns JWT | Public |
| POST | `/api/auth/refresh` | Refresh access token | Public |
| POST | `/api/auth/logout` | Revoke tokens | Required |
| GET | `/api/auth/me` | Current user profile | Required |
| GET | `/api/users` | List all users | Required |
| PATCH | `/api/users/{id}/role` | Change user role | Admin only |

### Bug Service (port 8082)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/projects` | List projects | Required |
| POST | `/api/projects` | Create project | Required |
| GET | `/api/projects/{id}/bugs` | List bugs (paginated + filtered) | Required |
| POST | `/api/projects/{id}/bugs` | Report a bug | Required |
| PUT | `/api/projects/{id}/bugs/{bugId}` | Update bug | Required |
| DELETE | `/api/projects/{id}/bugs/{bugId}` | Delete bug | Required |
| GET | `/api/projects/{id}/bugs/stats` | Bug stats by status/priority | Required |
| GET | `/api/projects/{id}/bugs/search?q=` | Full-text search | Required |
| GET | `/api/projects/{id}/bugs/{bugId}/activity` | Audit log | Required |
| GET | `/api/projects/{id}/bugs/{bugId}/duplicates` | AI duplicate check | Required |
| POST | `/api/projects/{id}/bugs/{bugId}/attachments` | Upload file | Required |
| GET | `/api/bugs/{bugId}/comments` | List comments | Required |
| POST | `/api/bugs/{bugId}/comments` | Add comment | Required |

---

## ⚙️ Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev key) | **Change in production** — min 256 bits |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | PostgreSQL password |
| `OPENAI_ENABLED` | `false` | Set `true` to enable AI features |
| `OPENAI_API_KEY` | — | OpenAI API key for AI triage |
| `SENDGRID_API_KEY` | — | SendGrid key for email notifications |
| `FROM_EMAIL` | `noreply@trackforge.io` | Sender email address |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO credentials |
| `GF_SECURITY_ADMIN_PASSWORD` | `admin` | Grafana password |

---

## 🤖 AI Features

AI features are **disabled by default** and work without an API key using heuristic fallback.

To enable full AI:
```env
OPENAI_ENABLED=true
OPENAI_API_KEY=sk-your-key-here
```

| Feature | How it works |
|---|---|
| **Bug triage** | Sends title + description to gpt-4o-mini, returns suggested priority and severity |
| **Duplicate detection** | Embeds bug text with text-embedding-3-small, stores in pgvector, finds similar bugs with cosine similarity |
| **Heuristic fallback** | Keyword-based priority suggestion when AI is disabled |

---

## 🔄 CI/CD Pipeline

```
Push to main
    │
    ├── Test Auth Service  (JUnit + PostgreSQL)
    ├── Test Bug Service   (JUnit + PostgreSQL)
    ├── Test & Lint Frontend (TypeScript check + npm build)
    │
    └── Build & Push Docker Images → GitHub Container Registry (GHCR)
            ├── trackforge-auth-service:latest
            ├── trackforge-bug-service:latest
            ├── trackforge-notification-service:latest
            ├── trackforge-api-gateway:latest
            └── trackforge-frontend:latest
```

---

## 🐛 Known Issues

- [#1] All users register as DEVELOPER — admin bootstrap required manually via Swagger
  - Workaround: Use `PATCH /api/users/{id}/role` after registering to set correct roles

---

## 👩‍💻 Author

**Ushani Saubhagya**
Software Engineering Undergraduate — University of Kelaniya, Sri Lanka

---

<div align="center">
  <sub>Built with ☕ Java, ⚛️ React, and lots of debugging</sub>
</div>
