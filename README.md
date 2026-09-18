# Chexy — Backend

Spring Boot API for **Chexy**, a chess platform with a classic mode and an RPG mode (armies,
gold, special abilities, tie resolution). One of four repositories:

| Repo | Role |
|---|---|
| **Chexy-B** (this one) | Spring Boot 3.4 API, realtime, auth, game orchestration |
| [Chexy-F](https://github.com/yosrikhiari/Chexy-F) | React 18 + Vite client |
| [Chexy-M](https://github.com/yosrikhiari/Chexy-M) | Flask AI service (Stockfish bots, opening detection, RPG enemy armies) |
| [Chexy-Deployment](https://github.com/yosrikhiari/Chexy-Deployment) | Kubernetes manifests + Jenkins pipeline |

## What it does

- **Classic chess** — game sessions, moves, timers over WebSocket, spectators, game history,
  tie resolution, matchmaking and invites, tournaments.
- **RPG mode** — enhanced games with armies, gold management, player actions and AI-generated
  enemy armies (from Chexy-M).
- **Social** — users, friendships, chat, realtime notifications.
- **Analytics** — per-game analytics with scheduled cleanup.

## Stack

- **Java 17, Spring Boot 3.4** — `Controller` / `Service` / `Repository` / `Models` packages
- **Keycloak** — OAuth 2.0 / OIDC; the app talks to Keycloak's admin client for user management
- **MongoDB** — game state, sessions, history, RPG data
- **PostgreSQL** — Keycloak's own database (the application data lives in MongoDB)
- **Apache Kafka** — game event stream (`GameEventProducer`)
- **RabbitMQ** — messaging (`RabbitMQMessageService`)
- **WebSocket (STOMP)** — realtime boards, timers, chat
- **Docker Compose** — `postgres`, `mongodb`, `keycloak`, `backend`, `frontend`, `ai-model`

## Run

```bash
# full stack (Keycloak realm is imported from realm-export.json)
docker compose up --build

# Kafka variant
docker compose -f docker-compose-kafka.yml up --build

# backend only
cd backend && ./mvnw spring-boot:run
```

Keycloak realm export: `keycloak/realm-export.json`. Backend config:
`backend/src/main/resources/application.properties`.

## Layout

```
backend/
  src/main/java/.../Config        security, Keycloak, WebSocket, Kafka/RabbitMQ config
  src/main/java/.../Controller    REST + WebSocket controllers (game, RPG, tournament, chat, friends…)
  src/main/java/.../Service       game orchestration, matchmaking, realtime, analytics
  src/main/java/.../Repository    MongoDB repositories
  src/main/java/.../Models        entities and DTOs
  src/test/java                   tests
keycloak/                          Keycloak image + realm export
mongodb/, postgres/                database images
docker-compose.yml, docker-compose-kafka.yml
```

Built as a side project (2025).
