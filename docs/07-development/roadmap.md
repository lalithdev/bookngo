# Five-Day Execution Roadmap

## Objective

Complete a technically defensible Review 1 implementation of BookNGo within
five focused development days.

The project will prioritize correctness of the core booking engine and the
microservices architecture over unnecessary feature breadth.

The frozen business microservices are:
1. User Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

Infrastructure:
7. Eureka Server
8. API Gateway

---

# DAY 1 — Requirements, Decomposition, Architecture, Database and API Design [COMPLETED / FROZEN]

**Status: COMPLETED & FROZEN**
*The current next phase is implementation.*

All foundational design phases have been completed and frozen:
- Phase 1: Requirements baseline frozen
- Phase 2: Service decomposition finalized into 6 business microservices and 2 infrastructure services
- Phase 3: Architecture, topology, and inter-service communication patterns frozen
- Phase 4: Data model and database-per-service boundaries frozen (`user_db`, `movie_db`, `theatre_db`, `show_db`, `booking_db`, `payment_db`)
- Phase 5: API contracts and OpenAPI specifications frozen

---

# DAY 2 — Implementation Foundation

Implement the backend foundation and project skeleton:

- Project skeleton
- Eureka Server
- API Gateway
- Configuration foundation
- JWT/security foundation
- Service skeletons
- PostgreSQL connectivity

---

# DAY 3 — Concurrency and Real-Time

Focus on the primary technical challenge:

- Show-specific seat inventory
- Seat hold
- Expiry (five-minute hold)
- Concurrency control
- Idempotency
- Booking

---

# DAY 4 — Payment, Ticket and Frontend

Implement:

- Payment simulation
- Ticket generation
- Cancellation/refund simulation
- Frontend

---

# DAY 5 — Integration, Testing and Review

Complete:

- Integration testing
- Concurrency testing
- Failure testing
- Deployment
- Review preparation

---

# Priority Order

If time becomes constrained, preserve functionality in this order:

1. No double booking.
2. Seat hold and expiry.
3. Booking workflow.
4. JWT authentication.
5. Eureka.
6. API Gateway.
7. Service-to-service communication.
8. Payment simulation.
9. Ticket.
10. Cancellation/refund.
11. Real-time updates.
12. UI refinement.

Correctness of the booking engine takes priority over cosmetic features.