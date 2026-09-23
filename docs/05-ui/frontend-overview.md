# Frontend Overview

## Purpose

The BookNGo frontend is the customer-facing web application for
discovering movies, selecting shows and seats, creating bookings,
processing payments, and viewing tickets and booking history.

## Technology

- React
- JavaScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query

## Backend Integration

The frontend communicates only through the API Gateway.

Frontend
    ↓
API Gateway :8080
    ↓
BookNGo microservices

The frontend must never communicate directly with individual
microservice ports.

## Source of Truth

Backend API contracts are defined in:

docs/03-api/openapi.yaml

The frontend must not invent API endpoints, request fields,
response fields, or business rules.

## Core Principle

The frontend is responsible for presentation, interaction,
navigation, and user experience.

The backend remains authoritative for:

- seat availability
- holds
- hold expiry
- booking state
- payment state
- pricing
- concurrency
- authorization
- cancellation rules