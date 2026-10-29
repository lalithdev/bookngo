import { Routes, Route, Navigate } from 'react-router';
import { MainLayout } from '../components/layout/MainLayout';
import { HomePage } from '../pages/HomePage';
import { MoviesPage } from '../pages/MoviesPage';
import { MovieDetailPage } from '../pages/MovieDetailPage';
import { ShowDiscoveryPage } from '../pages/ShowDiscoveryPage';
import { SeatSelectionPage } from '../pages/SeatSelectionPage';
import { LoginPage } from '../pages/LoginPage';
import { CheckoutPage } from '../pages/CheckoutPage';
import { PaymentProcessingPage } from '../pages/PaymentProcessingPage';
import { BookingConfirmationPage } from '../pages/BookingConfirmationPage';
import { DigitalTicketPage } from '../pages/DigitalTicketPage';
import { BookingHistoryPage } from '../pages/BookingHistoryPage';
import { BookingDetailPage } from '../pages/BookingDetailPage';
import { PaymentHistoryPage } from '../pages/PaymentHistoryPage';
import { RefundStatusPage } from '../pages/RefundStatusPage';
import { ProfilePage } from '../pages/ProfilePage';
import { OperatorPortalPage } from '../pages/OperatorPortalPage';
import { AdminPortalPage } from '../pages/AdminPortalPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        {/* Core Discovery Routes */}
        <Route index element={<HomePage />} />
        <Route path="movies" element={<MoviesPage />} />
        <Route path="movies/:movieId" element={<MovieDetailPage />} />
        <Route path="movies/:movieId/shows" element={<ShowDiscoveryPage />} />

        {/* Seat Selection & Hold */}
        <Route path="shows/:showId/seats" element={<SeatSelectionPage />} />

        {/* Authentication */}
        <Route path="login" element={<LoginPage />} />
        <Route path="profile" element={<ProfilePage />} />

        {/* Order Review, Hold Timer & Payment */}
        <Route path="checkout/:bookingId" element={<CheckoutPage />} />
        <Route path="checkout/:bookingId/processing" element={<PaymentProcessingPage />} />

        {/* Confirmation & Digital Ticket Passes */}
        <Route path="confirmation/:bookingId" element={<BookingConfirmationPage />} />
        <Route path="bookings/:bookingId" element={<BookingDetailPage />} />
        <Route path="bookings/:bookingId/ticket" element={<DigitalTicketPage />} />
        <Route path="tickets" element={<Navigate to="/history" replace />} />

        {/* History, Payments & Refunds */}
        <Route path="history" element={<BookingHistoryPage />} />
        <Route path="payments/history" element={<PaymentHistoryPage />} />
        <Route path="refunds" element={<RefundStatusPage />} />

        {/* Operational Portals */}
        <Route path="operator" element={<OperatorPortalPage />} />
        <Route path="admin" element={<AdminPortalPage />} />

        {/* 404 Fallback */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

export default AppRoutes;
