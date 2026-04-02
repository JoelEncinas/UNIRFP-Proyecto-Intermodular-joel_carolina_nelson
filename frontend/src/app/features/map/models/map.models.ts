import { Bike } from '../../../shared/domain/bike.model';
import { BookingSummary } from '../../../shared/domain/booking.model';
import { Station } from '../../../shared/domain/station.model';

export type { BookingStatus, BookingSummary } from '../../../shared/domain/booking.model';
export type MapPaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
export type BookingUnlockPaymentMethod = 'SALDO' | 'STRIPE';
export type MapStripePaymentReturnState = 'success' | 'cancel' | null;

export interface MapCoordinate {
  latitude: number;
  longitude: number;
}

export interface CreateBookingRequest {
  userId: number;
  bikeId: number;
  expiryTime: string | null;
  paymentMethod: BookingUnlockPaymentMethod;
}

export interface StripeUnlockSessionData {
  paymentId: number;
  sessionId: string;
  checkoutUrl: string;
  paymentStatus: MapPaymentStatus;
}

export interface BookingCreatePaymentResponse {
  booking: BookingSummary;
  paymentMethod: BookingUnlockPaymentMethod;
  paymentStatus: MapPaymentStatus;
  stripe: StripeUnlockSessionData | null;
}

export interface ReturnBookingRequest {
  stationId: number;
}

export interface BookingStripeSessionRequest {
  bookingId: number;
  sessionId: string;
}

export interface MapCurrentUser {
  id: number;
  balance: number;
}

export interface MapPaymentConfig {
  unlockFee: number;
  currency: string;
  minTopUpAmount: number;
}

export interface MapLoadSnapshot {
  stations: Station[];
  allBikes: Bike[];
  availableBikes: Bike[];
  bookings: BookingSummary[];
  me: MapCurrentUser;
  paymentConfig: MapPaymentConfig;
}
