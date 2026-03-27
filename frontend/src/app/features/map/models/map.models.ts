import { Bike } from '../../../shared/domain/bike.model';
import { BookingSummary } from '../../../shared/domain/booking.model';
import { Station } from '../../../shared/domain/station.model';

export type { BookingStatus, BookingSummary } from '../../../shared/domain/booking.model';

export interface MapCoordinate {
  latitude: number;
  longitude: number;
}

export interface CreateBookingRequest {
  userId: number;
  bikeId: number;
  expiryTime: string | null;
}

export interface ReturnBookingRequest {
  stationId: number;
}

export interface MapLoadSnapshot {
  stations: Station[];
  allBikes: Bike[];
  availableBikes: Bike[];
  bookings: BookingSummary[];
}
