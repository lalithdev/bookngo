# Solution Overview

## Overview

BookNGo will be implemented as a distributed microservices-based application.

The system will separate business capabilities into independently deployable
services while maintaining strong consistency around the critical seat
allocation operation.

The exact business service boundaries will be finalized through a dedicated
Service Decomposition Analysis after the requirements baseline is frozen.

## High-Level System

```text
Customer
   │
   ▼
Frontend
   │
   ▼
API Gateway
   │
   ├──────────────► Business Services
   │
   └──────────────► Authentication / Security
                         │
                         ▼
                    Service Discovery
                       (Eureka)