#START
# Frontend State Management Specification

## 1. Overview and Core Philosophy
In BookNGo, state management is designed around one strict architectural rule:

> **The frontend must never become a second business-rule engine, and it must never store authoritative domain state in global client memory.**

The backend microservices remain the sole authority for seat availability, pricing snapshots, hold validity, booking lifecycles, and payment outcomes. The frontend is an interactive projection of this server state.

Frontend state is strictly partitioned into six distinct categories:
1. **Server State** (TanStack Query)
2. **Local UI Interaction State** (React `useState`, `useReducer`)
3. **Authentication & Session State** (React `AuthContext`)
4. **Navigation & URL State** (React Router)
5. **Form State** (Controlled inputs)
6. **Temporary Booking Interaction State** (Session draft)

---

## 2. State Categorization Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND STATE LANDSCAPE                        │
├──────────────────────────┬─────────────────────────────────────────────┤
│ 1. Server State          │ TanStack Query v5                           │
│    (Authoritative Cache) │ Movies, Shows, Seat Inventory, Holds,       │
│                          │ Bookings, Tickets, Payments, Profile        │
├──────────────────────────┼─────────────────────────────────────────────┤
│ 2. Local UI State        │ React useState / useReducer                 │
│    (Transient View)      │ Modals, Drawers, Zoom level, Active tabs    │
├──────────────────────────┼─────────────────────────────────────────────┤
│ 3. Auth & Session State  │ AuthContext + LocalStorage                  │
│    (Identity)            │ JWT Token, User Identity, Roles             │
├──────────────────────────┼─────────────────────────────────────────────┤
│ 4. Navigation State      │ React Router (URL / SearchParams)           │
│    (Routable State)      │ Active filters (city, date, genre, search)  │
├──────────────────────────┼─────────────────────────────────────────────┤
│ 5. Form State            │ Controlled React State                      │
│    (Input Drafts)        │ Phone OTP inputs, Profile fields            │
├──────────────────────────┼─────────────────────────────────────────────┤
│ 6. Temporary Booking     │ Session Draft (Ephemeral)                   │
│    (Pre-hold Selection)  │ Local selected physicalSeatIds (Max 6)      │
└──────────────────────────┴─────────────────────────────────────────────┘
```

---

## 3. Server State Architecture (TanStack Query)

### Query Key Factory
All TanStack Query keys are managed through a centralized, strictly typed key factory (`src/api/queryKeys.js`) to guarantee consistent cache addressing and invalidation:

```javascript
export const queryKeys = {
  // Movie Domain
  movies: {
    all: ['movies'],
    list: (filters) => ['movies', 'list', filters],
    detail: (movieId) => ['movies', 'detail', movieId],
  },
  // Theatre Domain
  theatres: {
    all: ['theatres'],
    list: (city) => ['theatres', 'list', { city }],
    detail: (theatreId) => ['theatres', 'detail', theatreId],
    screens: (theatreId) => ['theatres', theatreId, 'screens'],
    screenSeats: (screenId) => ['screens', screenId, 'seats'],
  },
  // Show Domain
  shows: {
    all: ['shows'],
    list: (filters) => ['shows', 'list', filters],
    detail: (showId) => ['shows', 'detail', showId],
    pricing: (showId) => ['shows', showId, 'pricing'],
    cancellationPolicy: (showId) => ['shows', showId, 'cancellationPolicy'],
  },
  // Booking Domain (Concurrency Sensitive)
  bookings: {
    seatInventory: (showId) => ['shows', showId, 'seat-inventory'],
    hold: (holdId) => ['holds', holdId],
    detail: (bookingId) => ['bookings', 'detail', bookingId],
    myList: () => ['bookings', 'me'],
    ticket: (bookingId) => ['bookings', bookingId, 'ticket'],
  },
  // Payment Domain
  payments: {
    status: (paymentId) => ['payments', paymentId, 'status'],
    refund: (paymentId) => ['payments', paymentId, 'refund'],
  },
  // User Domain
  user: {
    me: () => ['users', 'me'],
  },
};
```

---

## 4. Query Responsibilities and Cache Configurations

| Query Domain | Endpoint | Stale Time | Polling Interval | Cache Invalidation Trigger |
|---|---|---|---|---|
| **Movie List** | `GET /api/v1/movies` | 5 minutes | None | Manual refresh |
| **Movie Detail** | `GET /api/v1/movies/{id}` | 5 minutes | None | None |
| **Theatres** | `GET /api/v1/theatres` | 10 minutes | None | City selection change |
| **Physical Seats** | `GET /api/v1/screens/{id}/seats` | 15 minutes | None | Never (Static layout) |
| **Shows** | `GET /api/v1/shows` | 2 minutes | None | Date picker change |
| **Show Pricing** | `GET /api/v1/shows/{id}/pricing` | 5 minutes | None | None |
| **Cancellation Policy** | `GET /api/v1/shows/{id}/cancellation-policy` | 5 minutes | None | None |
| **Seat Inventory** | `GET /api/v1/shows/{id}/seat-inventory` | **0 seconds** | **5 seconds** | On hold mutation, 409 conflict, user focus |
| **Hold Details** | `GET /api/v1/holds/{id}` | **0 seconds** | None (Timer is local) | On checkout entry, payment initiation |
| **Payment Status** | `GET /api/v1/payments/{id}/status` | **0 seconds** | **2 seconds** | Stops immediately on `SUCCESS`, `FAILURE`, `CANCELLED` |
| **Booking Detail** | `GET /api/v1/bookings/{id}` | 30 seconds | None | On cancellation mutation |
| **My Bookings** | `GET /api/v1/bookings/me` | 1 minute | None | On booking confirmation, cancellation |
| **Digital Ticket** | `GET /api/v1/bookings/{id}/ticket` | 10 minutes | None | None (Immutable once issued) |
| **User Profile** | `GET /api/v1/users/me` | 5 minutes | None | On profile update mutation, login |

---

## 5. Polling and Realtime Synchronization

### Seat Inventory Polling:
- While on `/shows/:showId/seats`, the query hook polls `GET /api/v1/shows/{showId}/seat-inventory` every **5,000ms**:
  ```javascript
  export function useShowSeatInventory(showId) {
    return useQuery({
      queryKey: queryKeys.bookings.seatInventory(showId),
      queryFn: () => fetchSeatInventory(showId),
      staleTime: 0,
      refetchInterval: 5000,
      refetchIntervalInBackground: false, // Pauses when tab is backgrounded
    });
  }
  ```
- **Merging Rule**: When background polling returns updated server inventory, the local draft selection (`selectedPhysicalSeatIds`) is preserved unless a selected seat is reported as `HELD` (by another user) or `BOOKED`. In that case, the UI displays a conflict toast and drops that specific seat from local selection.

### Payment Status Polling:
- While on `/checkout/:bookingId/processing`, the query hook polls `GET /api/v1/payments/{paymentId}/status` every **2,000ms**:
  ```javascript
  export function usePaymentStatus(paymentId) {
    return useQuery({
      queryKey: queryKeys.payments.status(paymentId),
      queryFn: () => fetchPaymentStatus(paymentId),
      staleTime: 0,
      refetchInterval: (query) => {
        const status = query.state.data?.status;
        // Stop polling on terminal states
        if (['SUCCESS', 'FAILURE', 'CANCELLED', 'TIMEOUT', 'UNKNOWN'].includes(status)) {
          return false;
        }
        return 2000;
      },
    });
  }
  ```

---

## 6. Mutations and Idempotency Architecture

### Booking and Payment Idempotency Keys:
Idempotency keys protect critical state-changing POST requests against duplicate processing caused by network retries.
- **Hold Creation**: When the user clicks "Proceed to Book", the frontend generates a UUIDv4 key stored in local component state:
  ```javascript
  const idempotencyKey = useRef(crypto.randomUUID()).current;
  ```
- If the request times out or encounters a network glitch and the user clicks "Retry", the **exact same key** is re-transmitted.
- A new key is generated only when the user explicitly modifies their seat selection or initiates a brand new booking attempt.

### Mutation Invalidation Rules:
```text
Mutation: Create Hold (POST /api/v1/shows/{showId}/holds)
  └─ On Success:
       └─ Invalidate: ['shows', showId, 'seat-inventory']
       └─ Set Data: ['holds', holdId]
       └─ Navigate: /checkout/:bookingId
  └─ On 409 Conflict:
       └─ Invalidate: ['shows', showId, 'seat-inventory']
       └─ Deselect conflicting seat
       └─ Display Conflict Toast

Mutation: Initiate Payment (POST /api/v1/payments)
  Payload: { "bookingId": "<bookingId>", "providerName": "SIM_SUCCESS" }
  *Rule*: Client sends ONLY bookingId and verified providerName (SIM_SUCCESS, SIM_FAILURE, SIM_CANCELLED, SIM_UNKNOWN).
  *Rule*: Frontend NEVER sends amount or currency.
  └─ On Success:
       └─ Set Data: ['payments', paymentId, 'status']
       └─ Invalidate: ['bookings', 'detail', bookingId]
       └─ Navigate: /checkout/:bookingId/processing

Mutation: Cancel Booking (POST /api/v1/bookings/{bookingId}/cancel)
  └─ On Success:
       └─ Invalidate: ['bookings', 'detail', bookingId]
       └─ Invalidate: ['bookings', 'me']
       └─ Invalidate: ['shows', showId, 'seat-inventory']
```

---

## 7. Local UI Interaction State

### Seat Map Selection State (`useSeatSelectionState`):
Managed via local React `useState`:
```javascript
// Local draft: Array of physicalSeatId strings (max 6)
const [selectedSeatIds, setSelectedSeatIds] = useState([]);

const toggleSeat = (seatId) => {
  setSelectedSeatIds((prev) => {
    if (prev.includes(seatId)) {
      return prev.filter((id) => id !== seatId);
    }
    if (prev.length >= 6) {
      showToast('Maximum 6 seats allowed per booking', 'warning');
      return prev;
    }
    return [...prev, seatId];
  });
};
```
- **Rule**: `selectedSeatIds` lives solely in component memory or temporary session storage. It is NEVER written into the TanStack Query cache as authoritative inventory.

---

## 8. Authentication and Session State

### `AuthContext` Responsibilities:
- Holds the authenticated user object and JWT string:
  `{ user, token, isAuthenticated, login, logout, roles }`
- Token is mirrored to `localStorage.getItem('bookngo_token')` upon login.
- **Logout Cache Invalidation**: When `logout()` is invoked, the TanStack `queryClient.clear()` method is called immediately. This completely wipes all cached user profiles, booking histories, holds, and ticket passes from browser memory, preventing data leaks between different users on shared machines.

---

## 9. Request Cancellation (AbortController)
- All query functions and mutations pass the native `signal` provided by TanStack Query into the underlying `fetch` / Axios call.
- When a user navigates away from a screen (e.g., leaving `/shows/:showId/seats` to return to `/movies`), active in-flight inventory requests are cleanly aborted, eliminating unnecessary network bandwidth usage.

---

## 10. Forbidden State Practices
- ❌ **DO NOT** store `show_seat_inventory` in a global Redux/Zustand store.
- ❌ **DO NOT** optimistically confirm a booking before receiving `Booking.status === 'CONFIRMED'`.
- ❌ **DO NOT** compute final payable amounts in client state; always display the backend `SeatHoldResponse.totalAmount` or `PaymentResponse.amount`.
- ❌ **DO NOT** send `amount` or `currency` from the frontend to `POST /api/v1/payments`.
- ❌ **DO NOT** use browser `Date.now()` to determine if a server hold is valid; the server's `normalExpiresAt` timestamp is the sole authority.
- ❌ **DO NOT** claim or assume external SMS or email notifications; surface confirmation exclusively via UI and tickets.
#END
