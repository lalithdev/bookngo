#START
# UX Specification: Interactive Seat Selection

## 1. Overview and Core Invariants
The seat selection screen (`/shows/:showId/seats`) is the central operational and concurrency hotspot of the BookNGo platform.

### Immutable Principles:
1. **Local Selection ≠ Server Hold**: Clicking a seat on the frontend creates only a transient, local client draft. It does **NOT** reserve, hold, or lock the seat in the database.
2. **Server Authority**: The backend database is the sole authority for whether a seat is `AVAILABLE`, `HELD`, or `BOOKED`.
3. **Atomic Multi-Seat Hold**: When the user clicks "Proceed to Book", the frontend requests an atomic hold for all selected seats simultaneously via `POST /api/v1/shows/{showId}/holds`. If even one seat has been acquired by a competitor, the entire hold operation is rejected with an HTTP 409 Conflict.
4. **No Double Booking**: The frontend must gracefully present conflicts and immediately refresh inventory.

---

## 2. Seat-Map Structure and Visual Anatomy

```text
                             ┌─────────────────────────┐
                             │       CINEMA SCREEN     │
                             │  (Curved Glowing Arc)   │
                             └────────────▲────────────┘
                                          │
                                   SCREEN THIS WAY
                                          │
            Row A  [A1] [A2] [A3] [A4]         [A5] [A6] [A7] [A8]  ── VIP
            Row B  [B1] [B2] [B3] [B4]         [B5] [B6] [B7] [B8]  ── VIP
                                     (Walkway)
            Row C  [C1] [C2] [C3] [C4]         [C5] [C6] [C7] [C8]  ── PREMIUM
            Row D  [D1] [D2] [D3] [D4]         [D5] [D6] [D7] [D8]  ── PREMIUM
                                     (Walkway)
            Row E  [E1] [E2] [E3] [E4]         [E5] [E6] [E7] [E8]  ── REGULAR
            Row F  [F1] [F2] [F3] [F4]         [F5] [F6] [F7] [F8]  ── REGULAR
```

### Components of the Seat Map:
1. **Screen Arc**:
   - Visual: A top curved gradient SVG line (`cyan-500` glow) representing the physical screen location.
   - Subtext: Centered uppercase label: `SCREEN THIS WAY`.
2. **Category Tiers**:
   - Screen sections partitioned by horizontal dividers indicating tier headers:
     - `VIP` (e.g., ₹450)
     - `PREMIUM` (e.g., ₹320)
     - `REGULAR` (e.g., ₹200)
3. **Rows and Columns**:
   - Rows are lettered alphabetically from front to back (`A`, `B`, `C`...).
   - Row identifiers are pinned to both the left and right margins of the grid for easy scanning.
   - Seats within a row are numbered sequentially (`1`, `2`, `3`...).
   - Aisle gaps are represented by empty grid columns.
4. **Legend Bar**:
   - Docked above the seat grid:
     - `Available` (Neutral outline)
     - `Selected` (Solid Emerald Green)
     - `Held by Others` (Amber striped pattern)
     - `Booked / Sold` (Solid dark slate, non-interactive)

---

## 3. Authoritative Seat States vs Local UI State

The seat rendering engine calculates the visual representation of each seat by joining physical configuration with live inventory and local draft state:

| State | Source | Visual Appearance | Interactive Behavior | Tooltip / Screen Reader |
|---|---|---|---|---|
| **AVAILABLE** | Server Inventory | Category-colored outline (`border-slate-600` / `border-purple-500` / `border-amber-500`) | Clickable. Toggles seat into local `SELECTED` state. | "Row {row} Seat {num}, {category}, ₹{price}. Available." |
| **SELECTED** | Client Local State | Solid vibrant green (`bg-emerald-500 text-slate-950 font-bold border-emerald-400 shadow-md shadow-emerald-500/30`) | Clickable. Deselects seat and returns it to `AVAILABLE`. | "Row {row} Seat {num}. Selected by you." |
| **HELD** | Server Inventory (`status: HELD`) | Diagonal amber stripes or solid amber border with muted center (`border-amber-500/40 text-amber-500/40 bg-amber-950/20`) | Non-clickable. Disabled cursor. | "Row {row} Seat {num}. Currently held by another customer." |
| **BOOKED** | Server Inventory (`status: BOOKED`) | Muted dark slate fill (`bg-slate-800/40 text-slate-700 border-transparent cursor-not-allowed`) | Non-clickable. Disabled cursor. | "Row {row} Seat {num}. Already booked." |
| **MAINTENANCE** | Physical Config (`status: MAINTENANCE`) | Dashed empty box or hidden | Disabled. | "Seat out of order." |

---

## 4. User Interaction Rules

### Selection and Deselection:
- Clicking an `AVAILABLE` seat appends its `physicalSeatId` to the local selection array.
- Clicking an already `SELECTED` seat removes it from the local selection array.
- **Maximum Seat Limit (FR-06)**:
  - A user can select a maximum of **6 seats** per booking attempt.
  - If a user clicks a 7th seat while 6 are already selected:
    - The seat is NOT selected.
    - A floating warning toast appears: `"You can select up to 6 seats per transaction."`
    - The seat icon shakes with a subtle 300ms CSS animation.

### Sticky Bottom Selection Drawer:
As soon as at least 1 seat is selected, a floating/sticky drawer appears at the bottom of the viewport:
- **Left Section**:
  - Displays selected seat badges: `[A10 ×] [A11 ×]` with instant click-to-remove action.
  - Displays seat count: `2 Seats Selected`.
- **Center Section**:
  - Shows subtotal price computation: `₹700 (Incl. of applicable taxes)`.
- **Right Section**:
  - Primary red action button: **"Proceed to Book"**.
  - Includes chevron arrow and responsive price label on mobile: `"Proceed to Book • ₹700"`.

---

## 5. Live Inventory Polling & Concurrency Handling

### 5-Second Background Polling:
- While the user is on the seat selection page, TanStack Query polls `GET /api/v1/shows/{showId}/seat-inventory` every **5 seconds**.
- Background refreshes are non-intrusive and do not display full-screen loading spinners.
- A tiny glowing pulse dot in the top navigation indicates: `"Live seat inventory synced"`.

### Stale Inventory Reconciliation:
If server inventory changes during polling:
1. **Unselected Seat Becomes Held/Booked**:
   - The seat's visual representation seamlessly transitions from `AVAILABLE` to `HELD` or `BOOKED`.
2. **Currently Selected Seat Becomes Held by Competitor**:
   - If another customer creates a hold on seat `A10` while the current user had it in their local draft:
     - The seat is automatically removed from the user's `selectedSeatIds`.
     - An alert toast is triggered immediately: `"Seat A10 was just reserved by another customer and removed from your selection."`
     - The bottom price subtotal recalculated automatically.

---

## 6. Proceed to Book & Concurrency Conflicts

When the customer clicks **"Proceed to Book"**:

```text
User clicks "Proceed to Book"
              ↓
Is user authenticated?
   ├─ NO  ──→ Save selection in sessionStorage ──→ Redirect to /login
   │
   └─ YES ──→ Disable button & show spinner
              │
              ▼
   POST /api/v1/shows/{showId}/holds (with Idempotency-Key)
              │
      ┌───────┴────────┐
      ▼                ▼
HTTP 201 Created   HTTP 409 Conflict
(Hold Acquired)    (Seat Contention)
      │                │
      │                ▼
      │      1. Show high-priority conflict modal:
      │         "Seat Conflict: One or more selected seats
      │          are no longer available."
      │      2. Invalidate & refetch seat inventory.
      │      3. Remove conflicting seats from draft.
      │      4. User stays on seat map to pick alternative.
      ▼
Navigate to /checkout/:bookingId
(Hold timer starts: 5 minutes)
```

---

## 7. Mobile and Touch Experience
On mobile viewports (< 768px):
- **Responsive Viewport Scaling**: The full cinema layout is initially scaled to fit the screen width using an SVG `viewBox`.
- **Touch Gestures**:
  - Pinch-to-zoom to enlarge specific seat clusters.
  - Two-finger panning to navigate across large auditoriums.
- **On-Screen Zoom Toolbar**:
  - Floating buttons `[+]`, `[-]`, and `[Center]` in the lower-left corner allow single-finger zooming and repositioning.
- **Touch Targets**:
  - Seats maintain a minimum 44px x 44px invisible touch hit-box around the 32px visual seat box to prevent accidental mistaps.
- **Drawer Behavior**:
  - The bottom summary collapses into a compact floating bar with a swipe-up gesture to reveal full seat breakdown and breakdown of taxes.

---

## 8. Accessibility and Keyboard Navigation (a11y)
- **Grid Role**: The seat container exposes `role="grid"` with rows marked `role="row"`.
- **Keyboard Navigation**:
  - Users can focus the seat map and navigate seats using standard `ArrowUp`, `ArrowDown`, `ArrowLeft`, `ArrowRight` keys.
  - Pressing `Space` or `Enter` toggles seat selection.
  - Pressing `Escape` clears all selected seats after confirmation.
- **Screen Reader Announcements**:
  - Dynamic `aria-live="polite"` region announces seat changes:
    - `"Selected Row C Seat 12, Regular tier, 200 rupees. Total 2 seats selected, 400 rupees."`
    - `"Deselected Row C Seat 12."`
  - Seat buttons include comprehensive accessibility metadata:
    `aria-label="Row C Seat 12, Regular Category, ₹200, Available"`
    `aria-checked="false"`
    `role="checkbox"`
#END
