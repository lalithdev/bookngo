# BookNGo — Environment Configurations

## 1. Purpose

This document defines configuration conventions across supported deployment environments for BookNGo.

Supported environments:
- `local` (Local Docker Compose / Developer machines)
- `review/test` (Academic Review-1 / Staging environment)
- `production` (Target production deployment)

---

## 2. Configuration Categories

Every environment must supply configuration values via external environment variables. No actual secrets or credentials are hardcoded into source code or repository documents.

### 2.1 Database Configuration (Database-per-Service)
Each of the six business microservices requires standard JDBC configuration variables:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME` (`user_db`, `movie_db`, `theatre_db`, `show_db`, `booking_db`, `payment_db`)
- `DB_USERNAME`
- `DB_PASSWORD`

### 2.2 Infrastructure & Discovery Configuration
- `EUREKA_SERVER_URL` (Eureka discovery registry address)
- `GATEWAY_HOST` / `GATEWAY_PORT` (Centralized Gateway entry point)
- `FRONTEND_API_BASE_URL` (Public URL pointing to API Gateway `/api/v1`)

### 2.3 Security & JWT Configuration
- `JWT_SECRET` (HMAC secret key used for signing and verifying tokens; must be environment-driven)
- `JWT_EXPIRATION_MS` (Token validity duration in milliseconds; values such as `86400000` are example/default local configurations, not a frozen security requirement)

### 2.4 Simulation & External Provider Configuration
- `OTP_SIMULATION_ENABLED` (`true` for Review-1 / test environments; `false` when a real SMS provider is attached)
- `PAYMENT_SIMULATION_ENABLED` (`true` for Review-1 / test environments; `false` when a real payment gateway is attached)

---

## 3. Environment Matrix

| Configuration Variable | `local` | `review/test` | `production` |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | `test` | `prod` |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka` | Set via environment | Set via environment |
| `JWT_EXPIRATION_MS` | 86400000 (example default) | 86400000 (example default) | Configured per policy |
| `OTP_SIMULATION_ENABLED` | `true` | `true` | Configured per integration |
| `PAYMENT_SIMULATION_ENABLED` | `true` | `true` | Configured per integration |
| `LOGGING_LEVEL_BOOKNGO` | `DEBUG` | `INFO` | `WARN` |
