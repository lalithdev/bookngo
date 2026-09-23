#START
# BookNGo Screen Specifications

## 1. Overview
This document specifies all frontend screens for the BookNGo web application. Each specification details routes, access levels, entry/exit paths, component compositions, exact API endpoints, comprehensive states (loading, empty, error, success), responsive behavior, accessibility criteria, and concurrency/security requirements.

---

## 2. Screen Specifications (1 through 20)

### Screen 1: Home
- **Route**: `/`
- **Purpose**: Welcoming landing page showcasing featured now-showing movies, active cinema locations, and quick search.
- **Access Requirements**: Public (No authentication required).
- **Primary User**: Guest / Moviegoer.
- **Entry Points**: Direct URL visit, logo click in header.
- **Exit / Navigation Paths**:
  - Click Movie Card -> `/movies/:movieId`
  - Click "Browse All Movies" -> `/movies`
  - Click City Selector -> Updates active city context
- **Data Required**: Active movies list from Movie Service (`GET /api/v1/movies?status=ACTIVE`); active city selection stored in client context for subsequent theatre and show discovery.
- **API Endpoints Used**:
  - `GET /api/v1/movies?status=ACTIVE`
  - `GET /api/v1/theatres?city={selectedCity}` (for resolving cinemas in the selected city)
- **Primary Components**: `HeroBannerCarousel`, `CitySelectorDropdown`, `NowShowingGrid`, `QuickSearchBar`, `Footer`.
- **Primary Actions**: Select city, search by movie title, click movie card.
- **Secondary Actions**: View promotions, read cancellation terms in footer.
- **Loading State**: `MovieCardSkeleton` grid (pulse animation).
- **Empty State**: Centered alert: "No movies currently available."
- **Error State**: `InlineAlert` with "Failed to load featured movies" and a "Retry" button.
- **Success State**: Cinematic poster grid with hover interactions.
- **Responsive Behavior**: 1-column mobile carousel, 2-column tablet grid, 4-column desktop layout.
- **Accessibility Requirements**: `alt` tags on all movie posters, keyboard accessible carousel controls.
- **Concurrency Considerations**: Read-only cache; 2-minute stale time.
- **Security Considerations**: Public endpoints; no sensitive data.

---

### Screen 2: Movies Catalog
- **Route**: `/movies`
- **Purpose**: Searchable, filterable catalog of all available films.
- **Access Requirements**: Public.
- **Primary User**: Customer.
- **Entry Points**: Header "Movies" link, "View All" button on Home.
- **Exit / Navigation Paths**: Click Movie Card -> `/movies/:movieId`.
- **Data Required**: Movies array filtered by language, genre, or title.
- **API Endpoints Used**:
  - `GET /api/v1/movies` (query params: `title`, `language`, `genre`, `status=ACTIVE`)
- **Primary Components**: `FilterSidebar` (Languages, Genres), `SearchInput`, `MovieGrid`, `PaginationControls`.
- **Primary Actions**: Type search query (debounced 300ms), toggle language/genre pills.
- **Secondary Actions**: Clear all filters.
- **Loading State**: Skeletons matching grid layout.
- **Empty State**: `Film` icon with message "No movies match your criteria" and "Reset Filters" button.
- **Error State**: Error alert with "Retry" action.
- **Responsive Behavior**: Mobile filter bottom drawer with toggle button; desktop sticky left sidebar.
- **Accessibility Requirements**: Proper form labels on search input, aria-pressed on filter pills.
- **Concurrency / Security**: Read-only public catalog.

---

### Screen 3: Movie Details
- **Route**: `/movies/:movieId`
- **Purpose**: Full movie overview including synopsis, runtime, language, format tags, and direct showtime CTA.
- **Access Requirements**: Public.
- **Primary User**: Customer.
- **Entry Points**: Movie card clicks.
- **Exit / Navigation Paths**:
  - Click "Book Tickets" -> `/movies/:movieId/shows`
  - Click Breadcrumb -> `/movies`
- **Data Required**: Movie entity.
- **API Endpoints Used**:
  - `GET /api/v1/movies/{movieId}`
- **Primary Components**: `MovieHeroBackdrop`, `PosterCard`, `MetadataBadges` (Duration, Language, Release Date), `SynopsisSection`, `BookTicketsStickyCTA`.
- **Primary Actions**: Click "Book Tickets" primary CTA.
- **Secondary Actions**: View trailer modal (if link available in metadata), share movie link.
- **Loading State**: Hero skeleton with simulated poster and text blocks.
- **Empty / Error State**: HTTP 404 displays "Movie Not Found" with link back to catalog.
- **Success State**: High-resolution backdrop with gradient overlay.
- **Responsive Behavior**: Mobile stacked layout with fixed bottom CTA bar; desktop side-by-side hero presentation.
- **Accessibility Requirements**: High contrast on text overlay against dark backdrop (`text-slate-100` over `bg-black/80`).

---

### Screen 4: Show/Theatre Selection
- **Route**: `/movies/:movieId/shows`
- **Purpose**: Select showtimes grouped by theatre for a chosen date.
- **Access Requirements**: Public.
- **Primary User**: Customer.
- **Entry Points**: "Book Tickets" CTA from Movie Details.
- **Exit / Navigation Paths**: Click Showtime Pill -> `/shows/:showId/seats`.
- **Data Required**: Shows array for selected movie and date, theatre entities, cancellation policies.
- **API Endpoints Used**:
  - `GET /api/v1/shows?movieId={movieId}&city={city}&date={date}`
  - `GET /api/v1/theatres/{theatreId}`
  - `GET /api/v1/shows/{showId}/cancellation-policy`
- **Primary Components**: `HorizontalDatePicker` (7-day calendar strip), `FormatFilterPills` (2D, 3D, IMAX), `TheatreShowList`, `ShowtimePill`.
- **Primary Actions**: Select date chip, click showtime pill.
- **Secondary Actions**: Filter by show format.
- **Loading State**: Skeleton cards representing theatre rows.
- **Empty State**: "No screenings scheduled for this date. Please select another day."
- **Error State**: Error banner with retry button.
- **Responsive Behavior**: Horizontally scrollable date bar with swipe gestures.
- **Accessibility Requirements**: Showtimes read as "Screen 1, 7:30 PM, English 2D, Free Cancellation available".
- **Concurrency Considerations**: Shows scheduled status (`SCHEDULED`) verified by backend.

---

### Screen 5: Customer Login
- **Route**: `/login`
- **Purpose**: Phone number entry for customer OTP authentication; link to Theatre Operator login.
- **Access Requirements**: Public (Guests only; authenticated users redirected to `/`).
- **Primary User**: Customer.
- **Entry Points**: "Sign In" button in header, automatic redirect when attempting "Proceed to Book".
- **Exit / Navigation Paths**:
  - Submit valid phone -> transitions to Screen 6 (OTP Verification)
  - Click "Operator Login" -> switches to Password Login mode
- **Data Required**: None.
- **API Endpoints Used**:
  - `POST /api/v1/auth/otp/request`
- **Primary Components**: `AuthCard`, `PhoneInput` with country code (+91), `SubmitButton`, `OperatorLoginToggle`.
- **Primary Actions**: Enter phone number and click "Send OTP".
- **Loading State**: Button spinner with `isPending` state.
- **Error State**: Inline text: "Please enter a valid phone number" or HTTP 429 "Too many requests".
- **Responsive Behavior**: Centered modal-style card on desktop; full-width mobile container.
- **Accessibility Requirements**: Form label explicitly bound to input; `aria-invalid` on validation failure.
- **Security Considerations**: Rate-limited at backend; no sensitive data transmitted.

---

### Screen 6: OTP Verification
- **Route**: `/login/verify` (or inline step within `/login`)
- **Purpose**: Enter 6-digit OTP to authenticate and receive JWT.
- **Access Requirements**: Public.
- **Primary User**: Customer.
- **Entry Points**: Successful submission on Screen 5.
- **Exit / Navigation Paths**:
  - Successful verification -> Navigates to `returnUrl` (e.g., `/shows/:showId/seats` or `/history`)
  - Click "Change Phone" -> Returns to Screen 5
- **Data Required**: Active phone number from step 1.
- **API Endpoints Used**:
  - `POST /api/v1/auth/otp/verify`
  - `POST /api/v1/auth/otp/request` (for resend)
- **Primary Components**: `OtpInputGroup` (6 individual auto-advancing input cells), `CountdownTimer` (60s resend lock), `VerifyButton`.
- **Primary Actions**: Type OTP code, click "Verify & Continue".
- **Secondary Actions**: Click "Resend OTP" when countdown reaches 0.
- **Loading State**: Disabled inputs with verifying spinner.
- **Error State**: Red border on input boxes with message: "Invalid or expired code. Please try again."
- **Success State**: Brief success checkmark before instant redirect.
- **Security Considerations**: JWT securely stored in memory / local session; OTP never logged to console.

---

### Screen 7: Seat Selection
- **Route**: `/shows/:showId/seats`
- **Purpose**: Interactive visual seat map for browsing physical seat layout and selecting up to 6 available seats.
- **Access Requirements**: Public to view/select; authenticated to click "Proceed to Book".
- **Primary User**: Customer.
- **Entry Points**: Showtime pill click from Screen 4.
- **Exit / Navigation Paths**:
  - Click "Proceed to Book" -> Screen 8 (Hold/Review) via hold mutation
  - Click "Back" -> `/movies/:movieId/shows`
- **Data Required**: Show details, physical screen seats, show seat inventory, show pricing.
- **API Endpoints Used**:
  - `GET /api/v1/shows/{showId}`
  - `GET /api/v1/screens/{screenId}/seats`
  - `GET /api/v1/shows/{showId}/seat-inventory` (polled every 5 seconds)
  - `GET /api/v1/shows/{showId}/pricing`
- **Primary Components**: `ScreenCurveIndicator`, `InteractiveSeatGrid`, `SeatCategoryLegend`, `SelectedSeatsDrawer`, `ZoomPanToolbar`.
- **Primary Actions**: Click seat to toggle `SELECTED` / `AVAILABLE`, click "Proceed to Book".
- **Secondary Actions**: Zoom in/out on mobile, reset pan.
- **Loading State**: `SeatMapSkeleton` with pulse effect.
- **Error State**: "Failed to load seat layout" with reload button.
- **Concurrency Considerations**:
  - Local selection does NOT create a server hold.
  - Background polling updates seats held/booked by other users without deselecting current user's local draft.
  - Concurrency conflict (HTTP 409) displays alert and immediately invalidates inventory.
- **Accessibility Requirements**: Grid navigation with arrow keys; `aria-label` detailing row, seat number, category, price, and status.

---

### Screen 8: Hold / Booking Review
- **Route**: `/checkout/:bookingId`
- **Purpose**: Review active 5-minute hold, order breakdown, and proceed to payment selection.
- **Access Requirements**: Authenticated (`CUSTOMER` role, owner of booking).
- **Primary User**: Customer.
- **Entry Points**: Successful hold creation from Screen 7.
- **Exit / Navigation Paths**:
  - Click "Pay" -> Screen 10 (Payment Processing)
  - Click "Cancel & Release" -> Screen 7 (Hold released)
  - Hold expires -> Modal redirect to Screen 7
- **Data Required**: Active hold details, booking details, show details.
- **API Endpoints Used**:
  - `GET /api/v1/holds/{holdId}`
  - `GET /api/v1/bookings/{bookingId}`
  - `DELETE /api/v1/holds/{holdId}` (if customer cancels)
- **Primary Components**: `HoldCountdownBanner` (5-minute timer), `BookingSummaryCard`, `SeatListPills`, `PriceBreakdownTable`, `PaymentMethodSelector`, `PayButton`.
- **Primary Actions**: Select payment simulation provider (`SIM_SUCCESS`, `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`), click "Pay ₹Total".
- **Secondary Actions**: Click "Cancel & Release Seats".
- **Loading State**: Card skeletons with loading shimmer.
- **Empty State**: N/A (Redirects to movies if booking invalid).
- **Error State**: Hold expired alert with "Select New Seats" CTA.
- **Concurrency Considerations**: Hold expiry timestamp from backend is authoritative. Timer is strictly informational. Frontend never supplies amount or currency to payment endpoint; backend derives amount authoritatively.
- **Security Considerations**: Ownership checked via JWT; booking amount verified against backend snapshot.

---

### Screen 9: Payment Selection
- **Route**: Integrated within `/checkout/:bookingId`.
- **Purpose**: Choose payment simulation provider (`SIM_SUCCESS` for standard success, or simulation tests `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
- **Access Requirements**: Authenticated Customer.
- **Primary User**: Customer.
- **API Endpoints Used**:
  - `POST /api/v1/payments` (with `Idempotency-Key` header and payload `{ bookingId, providerName }`)
- **Primary Components**: `PaymentMethodList`, `ProviderRadioGroup`, `SecurityAssuranceNotice`.
- **Primary Actions**: Select simulation provider, submit payment.
- **Concurrency Considerations**: Payment initiation activates 2-minute payment grace period on backend hold. Client sends NO client-computed price.

---

### Screen 10: Payment Processing
- **Route**: `/checkout/:bookingId/processing`
- **Purpose**: High-clarity waiting screen while payment provider processes the charge and webhook resolves.
- **Access Requirements**: Authenticated Customer.
- **Primary User**: Customer.
- **Entry Points**: Payment submission on Screen 8/9.
- **Exit / Navigation Paths**:
  - Status SUCCESS -> Screen 11 (Booking Confirmation)
  - Status FAILURE -> Failure card with retry CTA
  - Status TIMEOUT / UNKNOWN -> Recovery screen
- **Data Required**: `paymentId`, `bookingId`.
- **API Endpoints Used**:
  - `GET /api/v1/payments/{paymentId}/status` (polled every 2 seconds)
- **Primary Components**: `ProcessingRadarSpinner`, `StatusTextBanner`, `GraceTimerNotice`, `DoNotRefreshWarning`.
- **Primary Actions**: Wait for polling resolution.
- **Secondary Actions**: Click "Check Status Again" if polling times out.
- **Concurrency Considerations**: Stop polling immediately upon receiving terminal status (`SUCCESS`, `FAILURE`, `CANCELLED`).
- **Security Considerations**: Idempotent status checking; no sensitive bank credentials handled.

---

### Screen 11: Booking Confirmation
- **Route**: `/bookings/:bookingId/confirmation`
- **Purpose**: Celebrate successful ticket purchase, present summary, and inform customer that tickets are accessible in My Bookings / Ticket pass.
- **Access Requirements**: Authenticated Customer (Owner).
- **Primary User**: Customer.
- **Entry Points**: Automatic transition upon payment `SUCCESS`.
- **Exit / Navigation Paths**:
  - Click "View Digital Ticket" -> Screen 12 (`/bookings/:bookingId/ticket`)
  - Click "Home" -> `/`
- **Data Required**: Booking entity with status `CONFIRMED`.
- **API Endpoints Used**:
  - `GET /api/v1/bookings/{bookingId}`
- **Primary Components**: `SuccessCheckmarkAnimation`, `BookingReferenceCard`, `ShowSummary`, `ViewTicketButton`, `AddCalendarButton`.
- **Primary Actions**: Click "View Digital Ticket".
- **Secondary Actions**: Return to home.
- **Success State**: Crisp celebratory theme (`text-emerald-400`); explicitly states booking is confirmed and saved to account (no external SMS/email claims).

---

### Screen 12: Digital Ticket
- **Route**: `/bookings/:bookingId/ticket`
- **Purpose**: Authoritative entry pass for the cinema screen with QR code snapshot.
- **Access Requirements**: Authenticated Customer (Owner).
- **Primary User**: Customer.
- **Entry Points**: Confirmation screen, "My Bookings" list item.
- **Exit / Navigation Paths**: "Back to Bookings" -> `/history`.
- **Data Required**: Ticket entity (`ticketCode`, `historicalSnapshot`, `status`).
- **API Endpoints Used**:
  - `GET /api/v1/bookings/{bookingId}/ticket`
- **Primary Components**: `CinemaTicketPass` (perforated layout), `QRCodeDisplay` (visually encoding `ticketCode` for gate admission, without cryptographic claims), `TicketCodeBadge`, `SeatPills`, `PrintDownloadButton`.
- **Primary Actions**: Download/Print Ticket, show QR at theatre entrance.
- **Secondary Actions**: Back to My Bookings.
- **Loading State**: Ticket pass skeleton.
- **Error State**: 404 Ticket Not Issued -> Message directing user to contact support or refresh.
- **Responsive Behavior**: Scaled to fit mobile screens perfectly without horizontal scroll.

---

### Screen 13: Booking History
- **Route**: `/history`
- **Purpose**: Chronological list of user's past and upcoming cinema reservations.
- **Access Requirements**: Authenticated Customer.
- **Primary User**: Customer.
- **Entry Points**: "My Bookings" in top navigation.
- **Exit / Navigation Paths**: Click booking card -> Screen 14 (`/bookings/:bookingId`).
- **Data Required**: Array of user bookings; client coordinates with show/movie/theatre queries to resolve presentation metadata.
- **API Endpoints Used**:
  - `GET /api/v1/bookings/me`
  - Coordinated: `GET /api/v1/shows/{showId}`, `GET /api/v1/movies/{movieId}`, `GET /api/v1/theatres/{theatreId}` (as needed)
- **Primary Components**: `HistoryFilterTabs` (Upcoming, Past, Cancelled), `BookingCardList`, `BookingCardItem`.
- **Primary Actions**: Click booking card, click "View Ticket".
- **Loading State**: Vertical stack of card skeletons.
- **Empty State**: Ticket icon with "No bookings found" and "Explore Movies" CTA button.
- **Error State**: Error alert with "Retry".
- **Responsive Behavior**: Single-column card feed.

---

### Screen 14: Booking Details
- **Route**: `/bookings/:bookingId`
- **Purpose**: Comprehensive view of an individual booking record, payment reference, and cancellation controls.
- **Access Requirements**: Authenticated Customer (Owner) or Admin.
- **Primary User**: Customer.
- **Entry Points**: Click item in Booking History.
- **Exit / Navigation Paths**:
  - Click "View Ticket" -> Screen 12
  - Click "Cancel Booking" -> Opens Cancellation Modal (Screen 15)
- **Data Required**: Booking entity, show details, cancellation policy.
- **API Endpoints Used**:
  - `GET /api/v1/bookings/{bookingId}`
  - `GET /api/v1/shows/{showId}/cancellation-policy`
  - `GET /api/v1/payments/{paymentId}/refund` (if cancelled)
- **Primary Components**: `BookingStatusBadge`, `MovieSummaryCard`, `SeatsSnapshotTable`, `PaymentDetailsCard`, `CancelBookingCTA`.
- **Primary Actions**: View Ticket, Cancel Booking (if eligible).
- **Loading State**: Card skeletons.
- **Error State**: HTTP 403 Forbidden ("You do not have permission to view this booking").

---

### Screen 15: Booking Cancellation Modal / View
- **Route**: Modal within `/bookings/:bookingId` (or `/bookings/:bookingId/cancel`).
- **Purpose**: Confirm intent to cancel a confirmed booking and initiate refund according to policy.
- **Access Requirements**: Authenticated Customer (Owner).
- **Primary User**: Customer.
- **Entry Points**: "Cancel Booking" button on Screen 14.
- **Exit / Navigation Paths**:
  - Confirm Cancellation -> Updates Screen 14 to `CANCELLED`
  - Dismiss -> Closes modal
- **API Endpoints Used**:
  - `POST /api/v1/bookings/{bookingId}/cancel`
- **Primary Components**: `CancellationWarningDialog`, `PolicyRefundSummary`, `ConfirmCancelButton`, `KeepBookingButton`.
- **Primary Actions**: Click "Confirm Cancellation".
- **Secondary Actions**: Click "Keep Booking".
- **Loading State**: Disables buttons, shows "Processing cancellation...".
- **Error State**: HTTP 409: "Cancellation deadline has passed. This booking can no longer be cancelled."
- **Concurrency Considerations**: Concurrently checked against show start time and policy deadline.

---

### Screen 16: User Profile
- **Route**: `/profile`
- **Purpose**: View and update customer personal details (Full Name, Email, registered phone).
- **Access Requirements**: Authenticated User.
- **Primary User**: Customer.
- **Entry Points**: Header profile dropdown.
- **Exit / Navigation Paths**: "Sign Out" -> clears session and navigates to `/`.
- **Data Required**: User profile entity.
- **API Endpoints Used**:
  - `GET /api/v1/users/me`
  - `PUT /api/v1/users/me`
- **Primary Components**: `ProfileCard`, `FullNameInput`, `EmailInput`, `PhoneDisplayReadOnly`, `SaveButton`, `LogoutButton`.
- **Primary Actions**: Update name/email, click "Save Changes".
- **Secondary Actions**: Log out.
- **Loading State**: Profile form skeleton.
- **Success State**: Green toast: "Profile updated successfully".

---

### Screen 17: Unauthorized (403 Forbidden)
- **Route**: `/unauthorized`
- **Purpose**: Explain that the user lacks required role permissions to view the requested resource.
- **Access Requirements**: Public.
- **Primary Components**: `ShieldAlertIcon`, `ForbiddenTitle`, `ExplanationText`, `ReturnHomeButton`.
- **Primary Actions**: Return to Home.

---

### Screen 18: Not Found (404)
- **Route**: `*` (Catch-all)
- **Purpose**: Clean recovery screen for broken links or deleted movies/shows.
- **Access Requirements**: Public.
- **Primary Components**: `BrokenFilmReelIcon`, `Title404`, `BrowseMoviesCTA`.
- **Primary Actions**: Click "Browse Movies".

---

### Screen 19: Generic Error
- **Route**: Rendered via `GlobalErrorBoundary` or `/error`.
- **Purpose**: Gracefully handle unexpected JavaScript runtime exceptions.
- **Access Requirements**: Public.
- **Primary Components**: `AlertTriangleIcon`, `ErrorMessage`, `ReloadPageButton`.
- **Primary Actions**: Click "Reload Page".

---

### Screen 20: Service Unavailable (503 / Maintenance)
- **Route**: `/service-unavailable` (or rendered on gateway failure).
- **Purpose**: Inform users when backend microservices or API Gateway are temporarily unreachable.
- **Access Requirements**: Public.
- **Primary Components**: `WrenchIcon`, `MaintenanceTitle`, `Explanation`, `CheckAgainButton`.
- **Primary Actions**: Click "Check System Status".
#END
