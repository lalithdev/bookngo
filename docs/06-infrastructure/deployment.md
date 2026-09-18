# BookNGo — Deployment Architecture & Guide

## 1. Overview

BookNGo is designed as a set of six independent Spring Boot business microservices, supported by a React frontend and two infrastructure components (Eureka Server and API Gateway).

Each business service owns its own PostgreSQL database following the database-per-service paradigm.

---

## 2. Architecture Services Summary

### Business Services (Spring Boot)
1. **User Service** (`user_db`)
2. **Movie Service** (`movie_db`)
3. **Theatre Service** (`theatre_db`)
4. **Show Service** (`show_db`)
5. **Booking Service** (`booking_db`)
6. **Payment Service** (`payment_db`)

### Infrastructure Services
7. **Eureka Server** (Service Discovery, Port 8761)
8. **API Gateway** (Spring Cloud Gateway, Port 8080)

### Client
9. **Frontend** (Vite + React, Port 3000)

---

## 3. Local Development Orchestration (Docker Compose)

The primary mechanism for local development and integration testing is **Docker Compose**.

A `docker-compose.yml` file in the project root orchestrates the running containers:
- Database layer: One PostgreSQL instance may host six separately owned service databases for local development (`user_db`, `movie_db`, `theatre_db`, `show_db`, `booking_db`, `payment_db`). No cross-service database access is permitted.
- Eureka Server container.
- API Gateway container.
- 6 Spring Boot microservice containers.
- React Frontend container.

---

## 4. Configuration & Environment Variables

All environment-specific configuration, credentials, and secrets must be injected via environment variables.

### Environment Variable Principles
- **No hardcoded secrets**: Passwords, JWT secrets, and provider keys must never be committed to Git.
- **Configurable database URLs**: Each service connects to its owning database via standard environment variables (e.g., `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- **Dynamic Service Discovery**: Services locate Eureka via `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- **API Gateway Routing**: API Gateway routes requests based on registered Eureka service identifiers (`USER-SERVICE`, `MOVIE-SERVICE`, etc.).

---

## 5. Review-1 Deployment Strategy

For the academic Review-1 demonstration:
- **₹0 Cost Target**: Review-1 has a zero-cost target. Potential cloud deployment options include free-tier providers (such as Render, Railway, Fly.io, or AWS Free Tier), but no specific deployment provider has been selected. Local Docker-based demonstration remains acceptable if cloud deployment is not practical.
- **Simulated External Providers**:
  - OTP delivery is simulated via User Service logging/mocking to avoid SMS provider API costs.
  - Payment processing is simulated via Payment Service mock flows to avoid real payment gateway fees.
- No paid external infrastructure dependencies or additional microservices are introduced.
