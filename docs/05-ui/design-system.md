#START
# BookNGo Design System Specification

## 1. Design Goals
The BookNGo design system establishes a cohesive, immersive, cinematic visual language and interaction model. It ensures that every page feels part of a unified, production-grade cinema ticketing platform rather than a disconnected series of administrative forms.

Key goals:
- **Cinematic Atmosphere**: Evoke the premium ambience of modern movie theatres with sleek dark backgrounds, luminous accents, and crisp contrast.
- **Frictionless Ticket Purchase**: Provide high-clarity visual feedback during high-stress, time-sensitive moments (concurrency conflicts, 5-minute hold countdowns, payment grace periods).
- **Tailwind CSS Practicality**: Every token, component spec, and state transition maps directly to clean, standard Tailwind CSS utility patterns.
- **Strict Domain Alignment**: Visual states faithfully represent the backend domain models (`AVAILABLE`, `HELD`, `BOOKED`, `CONFIRMED`, `TICKET_ISSUED`, etc.) without fabricating non-existent states.

---

## 2. Visual Direction
BookNGo adopts a **Modern Dark Cinema Theme**:
- Deep obsidian and charcoal backdrops that minimize eye fatigue and let vibrant movie posters, screen curves, and seat categories command attention.
- Subtle glassmorphism (frosted translucent panels with 1px borders) for floating headers, selection summary drawers, and dialogs.
- Luminous crimson/amber accents reminiscent of stage lighting and cinema velvet.

---

## 3. Brand Personality
- **Cinematic**: Bold, dramatic, immersive, and visually focused on film art.
- **Trustworthy & Authoritative**: High precision during financial transactions and seat reservations.
- **Responsive & Dynamic**: Micro-interactions, smooth hover transitions, and unambiguous status indicators.

---

## 4. Color System
The color system is organized into foundational tokens implemented via modern Tailwind CSS v4 (`@import "tailwindcss";` with `@theme` block in CSS, avoiding legacy PostCSS / `tailwind.config.js` files).

### Base Neutrals (Dark Slate & Charcoal)
| Token Name | Hex Code | Tailwind Class | Semantic Usage |
|---|---|---|---|
| `bg-cinema-950` | `#0B0F17` | `bg-slate-950` | Deepest root viewport background |
| `bg-cinema-900` | `#111827` | `bg-gray-900` | Card, container, and modal panel surface |
| `bg-cinema-800` | `#1F2937` | `bg-gray-800` | Elevated surfaces, dropdowns, input backgrounds |
| `bg-cinema-700` | `#374151` | `bg-gray-700` | Hover states on secondary surfaces, dividers |
| `border-cinema` | `#2D3748` | `border-slate-800` | Subtle 1px borders defining cards and modals |
| `text-primary` | `#F9FAFB` | `text-gray-50` | Primary headings, titles, active labels |
| `text-secondary` | `#9CA3AF` | `text-gray-400` | Subtitles, show metadata, descriptions |
| `text-muted` | `#6B7280` | `text-gray-500` | Captions, disabled labels, timestamps |

### Brand Accents (Cinema Crimson & Amber)
| Token Name | Hex Code | Tailwind Class | Semantic Usage |
|---|---|---|---|
| `brand-primary` | `#E50914` | `bg-red-600` / `text-red-600` | Primary CTAs, active brand elements, key highlights |
| `brand-hover` | `#DC2626` | `hover:bg-red-700` | Hover state for primary buttons |
| `brand-glow` | `rgba(229, 9, 20, 0.25)` | `shadow-red-500/20` | Subtle glowing drop shadow for primary CTAs |
| `accent-gold` | `#F59E0B` | `bg-amber-500` / `text-amber-500` | VIP seats, ratings, promotional highlights |

---

## 5. Semantic Colors
Color tokens reserved strictly for status, domain inventory states, and feedback:

| Category | Tailwind Classes | Semantic Purpose |
|---|---|---|
| **Success** | `bg-emerald-500`, `text-emerald-400`, `border-emerald-500/30` | Confirmed bookings, issued tickets, payment success |
| **Warning** | `bg-amber-500`, `text-amber-400`, `border-amber-500/30` | Expiring holds (< 60s), payment grace active, pending status |
| **Danger / Error** | `bg-rose-500`, `text-rose-400`, `border-rose-500/30` | Payment failure, seat conflict, expired hold, cancellation |
| **Info** | `bg-cyan-500`, `text-cyan-400`, `border-cyan-500/30` | Informational announcements, policy notes, format tags |

---

## 6. Typography Hierarchy
BookNGo uses modern, legible sans-serif typography with strict scale and line heights:

| Level | Size (Desktop / Mobile) | Weight | Tailwind Class | Application |
|---|---|---|---|---|
| **Display 1** | 48px / 32px | Bold (700) | `text-3xl md:text-5xl font-bold tracking-tight` | Movie hero titles, booking success headline |
| **Heading 1** | 32px / 24px | Bold (700) | `text-2xl md:text-3xl font-bold` | Page titles (Movies, Shows, Seat Selection) |
| **Heading 2** | 24px / 20px | Semibold (600) | `text-xl md:text-2xl font-semibold` | Section headers (Select Date, Theatres) |
| **Heading 3** | 18px / 16px | Semibold (600) | `text-base md:text-lg font-semibold` | Card titles, theatre names, modal headers |
| **Body Large** | 16px / 15px | Regular (400) | `text-base` | Movie synopses, checkout breakdown items |
| **Body Regular** | 14px / 14px | Regular (400) | `text-sm` | Default UI text, input fields, descriptions |
| **Caption / Meta** | 12px / 11px | Medium (500) | `text-xs font-medium` | Seat labels, timestamps, badge labels |

---

## 7. Font Strategy
- **Primary Font**: `Inter`, `-apple-system`, `BlinkMacSystemFont`, `Segoe UI`, `Roboto`, `sans-serif`.
- Inter provides exceptional legibility at small sizes (crucial for seat map row/column labels and dense showtime tables) and balanced geometry for large cinematic banners.
- Loaded locally or via Google Fonts with `font-display: swap` to prevent layout shifts.

---

## 8. Spacing Scale
BookNGo adheres to Tailwind's 4px (0.25rem) base scale:
- `p-1` (4px), `p-2` (8px), `p-3` (12px), `p-4` (16px), `p-6` (24px), `p-8` (32px), `p-12` (48px).
- **Component Padding**: Cards use `p-4` on mobile, `p-6` on desktop.
- **Section Margins**: Vertical spacing between page sections uses `space-y-8` or `my-8`.

---

## 9. Border and Radius System
- **Radius Small (`rounded-md`, 6px)**: Badges, seat icons, tag pills, small inputs.
- **Radius Medium (`rounded-lg`, 8px)**: Buttons, text fields, dropdown menus.
- **Radius Large (`rounded-xl`, 12px)**: Cards, movie poster containers, checkout cards.
- **Radius Extra Large (`rounded-2xl`, 16px)**: Modals, bottom drawers, ticket passes.
- **Borders**: Subtle `1px` borders using `border border-slate-800` or `border border-white/10` to define shapes against dark backgrounds without heavy visual noise.

---

## 10. Shadows and Elevation
- **Elevation 0 (Flat)**: Background surfaces.
- **Elevation 1 (`shadow-md shadow-black/40`)**: Cards and list items.
- **Elevation 2 (`shadow-xl shadow-black/60`)**: Dropdowns, popovers, sticky footers.
- **Elevation 3 (`shadow-2xl shadow-black/90`)**: Dialogs, modals, floating action bars.
- **Brand Glow**: `shadow-lg shadow-red-600/20` applied to primary action buttons.

---

## 11. Layout and Container Rules
- **Global Container**: Centered with responsive horizontal gutters:
  `max-w-7xl mx-auto px-4 sm:px-6 lg:px-8`
- **Page Layout**: Flex column with sticky header, scrollable main content, and footer:
  `min-h-screen flex flex-col bg-slate-950 text-gray-100`

---

## 12. Responsive Breakpoints
Standard Tailwind breakpoints applied:
- `sm`: 640px — Phablets / Landscape mobile
- `md`: 768px — Tablets
- `lg`: 1024px — Laptops / Desktop
- `xl`: 1280px — Large Desktop monitors

---

## 13. Buttons
Buttons provide distinct visual hierarchy:
- **Primary**: `bg-red-600 hover:bg-red-700 text-white font-semibold px-6 py-2.5 rounded-lg shadow-lg shadow-red-600/20 transition duration-150 ease-in-out disabled:opacity-50 disabled:cursor-not-allowed`
- **Secondary**: `bg-slate-800 hover:bg-slate-700 text-gray-200 font-medium px-5 py-2.5 rounded-lg border border-slate-700`
- **Outline**: `bg-transparent hover:bg-slate-800 text-red-500 border border-red-600 px-5 py-2.5 rounded-lg`
- **Ghost**: `bg-transparent hover:bg-slate-800 text-gray-300 px-4 py-2 rounded-lg`
- **Danger**: `bg-rose-600 hover:bg-rose-700 text-white font-medium px-5 py-2.5 rounded-lg`
- **Loading State**: Displays a spinning `Loader2` SVG icon, sets `aria-busy="true"`, and disables pointer events.

---

## 14. Inputs
- Background `bg-slate-900 border border-slate-700 rounded-lg px-4 py-2.5 text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition`
- Error state: `border-rose-500 focus:ring-rose-500`

---

## 15. Selects
- Custom styled select with chevron indicator: `bg-slate-900 border border-slate-700 text-gray-200 rounded-lg px-4 py-2.5 pr-8 appearance-none focus:outline-none focus:ring-2 focus:ring-red-500`

---

## 16. Search Fields
- Enclosed with a magnifying glass search icon:
  `relative flex items-center w-full max-w-md`
  Icon: `absolute left-3 text-gray-500 w-5 h-5`
  Input: `pl-10 pr-4 py-2 bg-slate-900 border border-slate-700 rounded-lg`

---

## 17. Cards
- Core card: `bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-md shadow-black/40`

---

## 18. Movie Cards
- Poster container with 2:3 aspect ratio (`aspect-[2/3]`), rounded corners (`rounded-xl`), overflow hidden, and subtle scale-up on hover (`group-hover:scale-105 transition duration-300`).
- Gradient overlay on hover displaying language tag, genre pills, and "Book Tickets" CTA button.

---

## 19. Theatre and Show Cards
- Grouped by theatre: Header with theatre name, address pin, and distance/city.
- Showtime pills:
  - Available: `bg-slate-800 hover:bg-slate-700 border border-slate-700 text-emerald-400 hover:text-emerald-300 font-medium px-4 py-2 rounded-lg text-sm transition`
  - Cancellation Tag: Pill displaying `Free Cancellation` (if policy is `CANCELLABLE`) or `Non-Refundable`.

---

## 20. Badges and Status Indicators
Compact status tags:
- `AVAILABLE`: `bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-0.5 rounded-full text-xs font-semibold`
- `HELD`: `bg-amber-500/10 text-amber-400 border border-amber-500/20 px-2.5 py-0.5 rounded-full text-xs font-semibold`
- `BOOKED`: `bg-slate-800 text-gray-400 border border-slate-700 px-2.5 py-0.5 rounded-full text-xs font-semibold`
- `CONFIRMED`: `bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 px-3 py-1 rounded-full text-xs font-semibold`
- `CANCELLED`: `bg-rose-500/10 text-rose-400 border border-rose-500/20 px-2.5 py-0.5 rounded-full text-xs font-semibold`

---

## 21. Dialogs and Modals
- Fixed backdrop: `fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4`
- Modal content card: `bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150`

---

## 22. Toasts and Notifications
- Fixed viewport container: `fixed bottom-4 right-4 z-50 flex flex-col space-y-2 max-w-md w-full`
- Toast card: `bg-slate-900/95 border border-slate-800 rounded-xl p-4 shadow-xl flex items-start space-x-3`
  - Success Toast: green icon, title, description.
  - Conflict Toast: red warning icon, message ("Seat A10 was just booked by another user. Inventory updated.").

---

## 23. Tabs
- Tab bar: `flex space-x-2 border-b border-slate-800 pb-2`
- Active tab: `text-red-500 border-b-2 border-red-500 font-semibold pb-2`
- Inactive tab: `text-gray-400 hover:text-gray-200 font-medium pb-2`

---

## 24. Navigation
- Sticky top navigation bar: `sticky top-0 z-40 bg-slate-950/80 backdrop-blur-md border-b border-slate-800/80`
- Contains: BookNGo brand logo, city selector dropdown, movie search bar, and user profile / login action.

---

## 25. Header
- Brand logo: Bold typography with cinema icon and red accent dot (`BookNGo.`).
- Right actions: "My Bookings" link (for logged-in customers), "Sign In" button (for guests).

---

## 26. Footer
- Dark muted footer: `bg-slate-950 border-t border-slate-900 py-12 text-sm text-gray-500`
- Contains copyright, active city links, cancellation policy guide, and security notice.

---

## 27. Breadcrumbs
- Subtle path indicator above detail screens:
  `Movies > Inception > PVR Cinema > 07:30 PM`
  Styled with `text-xs text-gray-500 hover:text-gray-300 transition`.

---

## 28. Loading Skeletons
- Pulse animation applied to matching geometry: `animate-pulse bg-slate-800/60 rounded-lg`
- Skeletons prevent layout shifting during TanStack Query resolution.

---

## 29. Empty States
- Centered container with muted illustration icon (e.g., `Film`, `Ticket`, `CalendarX`), crisp title, informative subtext, and clear action button.

---

## 30. Error States
- Inline alert card: `bg-rose-950/30 border border-rose-500/30 rounded-xl p-4 text-rose-300 flex items-center space-x-3`
- Contains retry CTA button triggering `query.refetch()`.

---

## 31. Confirmation States
- Full-page or card layout with celebratory green badge, ticket code snapshot, and "Download Ticket / View in My Bookings" action.

---

## 32. Seat-Map Visual Language
The seat map strictly implements the domain categories and authoritative backend states:

### Seat Categories (from Physical Configuration & Show Pricing)
1. **VIP**: Top-tier luxury seats. Visual token: `border-amber-500/60 text-amber-400 bg-amber-500/10`
2. **PREMIUM**: Middle-tier prime viewing. Visual token: `border-purple-500/60 text-purple-400 bg-purple-500/10`
3. **REGULAR**: Standard theatre seating. Visual token: `border-slate-600 text-slate-300 bg-slate-800/60`

### Seat Interactive States
| State | Authoritative Backend Meaning | Visual Representation | User Interaction |
|---|---|---|---|
| **AVAILABLE** | Inventory status `AVAILABLE` | Clean outline corresponding to category color | Clickable; toggles to `SELECTED` |
| **SELECTED** | Local client draft (max 6 seats) | Solid green (`bg-emerald-500 border-emerald-400 text-slate-950 font-bold`) | Clickable; toggles back to `AVAILABLE` |
| **HELD (Other User)**| Inventory status `HELD`, hold active | Striped amber/charcoal fill, muted label | Non-clickable; tooltip "Currently Held" |
| **BOOKED** | Inventory status `BOOKED` | Dark muted slate fill (`bg-slate-800/40 text-slate-600 border-transparent`) | Disabled / Non-clickable |
| **MAINTENANCE / INACTIVE** | PhysicalSeat status `INACTIVE` | Empty space or dashed grey outline | Disabled / Non-clickable |

### Screen Representation
- Elegant curved SVG arc positioned at the top of the map with glow:
  `w-3/4 mx-auto h-2 bg-gradient-to-r from-transparent via-cyan-500/50 to-transparent rounded-full blur-[1px]`
  Label centered below: `SCREEN THIS WAY` in `text-[10px] tracking-widest text-gray-500 uppercase`.

---

## 33. Hold Timer Visual Language
The 5-minute hold timer is rendered as an informational floating countdown pill:
- **Normal (> 60s)**: `bg-slate-800/90 border border-slate-700 text-gray-200` with clock icon.
- **Urgent (< 60s)**: `bg-rose-950/80 border border-rose-500/50 text-rose-300 animate-pulse` with alert icon.
- Copy: `"Seats held for 04:32. Complete payment to confirm."`

---

## 34. Payment-State Visual Language
- **Payment Processing**: Centered animated spinner, pulsating cinema logo, and copy: `"Securing your seats and contacting payment provider..."`
- **Payment Simulation States**: Supports verified backend simulation controls (`SIM_SUCCESS`, `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
- **Payment Grace**: `"Additional 2-minute processing grace period active. Please do not close or refresh this window."`
- **Payment Unknown**: Amber alert box: `"Payment outcome pending confirmation. Reconciling with bank..."` with "Check Status" button.

---

## 35. Booking Confirmation Visual Language
- Designed to replicate a realistic cinema ticket pass:
  - White-on-slate ticket pass card with perforated dashed border divider.
  - Left pane: Movie poster thumbnail, title, format, theatre name, screen name, show date & time.
  - Right pane: High-contrast QR code representation (visually encoding the backend `ticketCode` for gate admission, without claiming cryptographic verification), alphanumeric `ticketCode`, seat labels (`A10, A11`), total amount paid.

---

## 36. Accessibility (a11y)
- Target: **WCAG 2.1 Level AA Compliance**.
- High contrast: Text meets minimum 4.5:1 ratio against dark backgrounds.
- Screen readers receive clear semantic attributes: `aria-expanded`, `aria-haspopup`, `aria-selected`, `aria-live`.

---

## 37. Keyboard Navigation
- All interactive elements are reachable via `Tab` / `Shift+Tab`.
- Seat-map supports full arrow-key grid navigation (`ArrowUp`, `ArrowDown`, `ArrowLeft`, `ArrowRight`) and `Space` / `Enter` for selection toggling.

---

## 38. Focus States
- Interactive elements display an unmistakable, accessible focus ring:
  `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950`

---

## 39. Reduced-Motion Behavior
- Honored via Tailwind's `motion-safe` and `motion-reduce` variants:
  `@media (prefers-reduced-motion: reduce)` disables scale-up animations, pulse effects, and transitions.

---

## 40. Mobile and Touch Interaction Rules
- Minimum touch target size: 44px x 44px for buttons, navigation links, and seat targets.
- The seat map incorporates touch gesture handling (pinch-to-zoom and two-finger pan) with an explicit zoom control toolbar (`[+]`, `[-]`, `[Reset]`) for single-finger accessibility.
#END
