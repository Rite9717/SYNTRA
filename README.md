# Syntra — A Secure Internal Messaging & Communication Platform

Syntra is a full-stack, self-hosted internal messaging platform built for organizations that want full control over their communications — no third-party mail services, no data leaving your infrastructure. It provides role-based private messaging alongside a real-time group chat room, all managed by an admin through a dedicated dashboard.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running Locally (Development)](#running-locally-development)
- [User Roles](#user-roles)
- [Pages & Routes](#pages--routes)
- [API Overview](#api-overview)
- [WebSocket / Chat](#websocket--chat)
- [Known Issues & Notes](#known-issues--notes)

---

## Overview

Syntra is designed for organizations — colleges, companies, NGOs, or any group — that need a private, controlled communication system without relying on public email providers. Accounts are **created by admins only**, ensuring no unauthorized access. Members can send private messages to each other and participate in a shared real-time chat room. Admins get a dedicated dashboard to monitor activity, manage users, and onboard new members.

---

## Features

### Regular Users
- Login with username and password
- View inbox with unread message badges
- Compose and send private messages to any member
- View sent messages and read receipts
- Real-time group chat room with join/leave notifications
- Auto-scroll and message history on chat load

### Admins
- All user features (inbox, compose, chat)
- Admin dashboard with live stats: total users, active users, total messages, messages today, unread messages
- User management table with search (by username, email, or name)
- View detailed user profile in a modal
- Activate / deactivate user accounts
- Create new user accounts with role assignment (User or Admin)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, React Router v6, Axios |
| Real-time Chat | STOMP over WebSocket (SockJS + @stomp/stompjs) |
| Backend | Java (Spring Boot) |
| Auth | JWT (Bearer token) |
| Reverse Proxy | Nginx |
| Containerization | Docker, Docker Compose |

---

## Project Structure

```
IMT/
├── backend/               # Spring Boot Java application
│   └── src/
├── front/                 # React (Vite) frontend
│   ├── src/
│   │   ├── components/
│   │   │   └── Navbar.jsx
│   │   ├── context/
│   │   │   └── AuthContext.jsx
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   ├── Inbox.jsx
│   │   │   ├── Compose.jsx
│   │   │   ├── SentMessages.jsx
│   │   │   ├── Chat.jsx
│   │   │   ├── AdminDashboard.jsx
│   │   │   ├── UserManagement.jsx
│   │   │   └── CreateUser.jsx
│   │   ├── services/
│   │   │   ├── api.js           # Axios instance + interceptors
│   │   │   ├── authService.js
│   │   │   ├── chatService.js
│   │   │   ├── messageService.js
│   │   │   └── userService.js
│   │   └── App.jsx              # Routes + auth guards
│   ├── nginx.conf
│   └── .env.production
└── docker-compose.yml
```

---

## Getting Started

### Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose installed
- Node.js 18+ (only needed for local dev without Docker)
- Java 17+ (only needed for local dev without Docker)

### Environment Variables

Create a `.env.production` file inside the `front/` directory:

```env
VITE_API_URL=http://<your-server-ip>/
```

> Make sure the URL ends with a trailing `/` since `api.js` constructs the base URL as `${VITE_API_URL}api`.

The backend typically expects its own configuration (database URL, JWT secret, etc.) via `application.properties` or environment variables in `docker-compose.yml`. Refer to `backend/src/main/resources/application.properties` for the full list.

### Running with Docker Compose

```bash
git clone https://github.com/Rite9717/IMT.git
cd IMT

# Build and start all services (frontend, backend, db)
docker compose up --build
```

Syntra will be available at `http://localhost` (port 80 via Nginx).

Nginx proxies:
- `/api/*` → Spring Boot backend on port 8080
- `/ws/*` → WebSocket endpoint on port 8080 (with upgrade headers)
- All other routes → React SPA (`index.html`)

### Running Locally (Development)

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd front
npm install
npm run dev
```

Create a `.env` file in `front/` for local dev:
```env
VITE_API_URL=http://localhost:8080/
```

---

## User Roles

| Role | Access |
|---|---|
| `ROLE_USER` | Dashboard, Inbox, Compose, Sent, Chat |
| `ROLE_ADMIN` | Admin Dashboard, User Management, Create User, Inbox (Messages), Chat |

- On login, users are redirected based on their role — admins go to `/admin/dashboard`, regular users go to `/dashboard`.
- Admin users visiting `/dashboard` are automatically redirected to `/admin/dashboard`.
- Route-level guards (`PrivateRoute`, `AdminRoute`) protect all authenticated and admin-only pages.
- **Self-registration is disabled by design** — the `/signup` route is removed. Only admins can create accounts, keeping the platform fully closed to outsiders.

---

## Pages & Routes

| Path | Component | Access |
|---|---|---|
| `/login` | Login | Public |
| `/dashboard` | Dashboard | User |
| `/inbox` | Inbox | User |
| `/compose` | Compose | User |
| `/sent` | SentMessages | User |
| `/chat` | Chat | User |
| `/admin/dashboard` | AdminDashboard | Admin only |
| `/admin/users` | UserManagement | Admin only |
| `/admin/create-user` | CreateUser | Admin only |
| `/` | Redirects to `/dashboard` | — |

---

## API Overview

All API calls go through the Axios instance in `services/api.js`, which automatically attaches the JWT token from `sessionStorage` to every request and handles 401 responses by clearing the session and redirecting to `/login`.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Login, returns JWT + user info |
| GET | `/api/messages/inbox` | Get inbox messages |
| GET | `/api/messages/sent` | Get sent messages |
| POST | `/api/messages` | Send a message |
| PUT | `/api/messages/:id/read` | Mark message as read |
| DELETE | `/api/messages/:id` | Delete a message |
| GET | `/api/messages/unread-count` | Get unread message count |
| GET | `/api/users` | Get all users (for compose dropdown) |
| GET | `/api/chat/history` | Get chat message history |
| GET | `/api/admin/dashboard/stats` | Admin stats (admin only) |
| GET | `/api/admin/users` | List all users (admin only) |
| POST | `/api/admin/users/create` | Create a new user (admin only) |
| PUT | `/api/admin/users/:id/toggle-status` | Activate/deactivate user (admin only) |

---

## WebSocket / Chat

Syntra's real-time chat uses **STOMP over SockJS**:

- **Connection endpoint:** `/ws`
- **Subscribe topic:** `/topic/public` — receives all chat messages
- **Send message:** `/app/chat.sendMessage`
- **Join notification:** `/app/chat.addUser`

Message types:
- `CHAT` — regular user message, displayed with sender name and timestamp
- `JOIN` / `LEAVE` — system messages shown as centered notices

Features:
- Automatic reconnect with 5-second delay on connection loss
- Chat history loaded from REST API on connect
- Messages limited to 2000 characters
- Timestamps handle microsecond precision from the backend by truncating to milliseconds before parsing

---

## Known Issues & Notes

- **Storage consistency:** `AuthContext` saves the token and user to `sessionStorage`. Any direct `localStorage` reads in components (`AdminDashboard`, `Chat`, `UserManagement`, `CreateUser`, `api.js`) should be updated to use `sessionStorage` to stay consistent.
- **Dashboard Chat card:** The Chat card on the regular user Dashboard currently shows "Coming soon..." and is not linked — the chat page itself is fully functional at `/chat`.
- **Signup page exists but is disabled:** `Signup.jsx` is present in the codebase but the route is intentionally removed from `App.jsx`. Account creation is admin-only by design.
