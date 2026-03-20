import { Bike } from './bike.model';
import { Station } from './station.model';

export type BookingStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface BookingSummary {
  id: number;
  userId: number;
  username: string;
  bikeId: number;
  bikeModel: string;
  startTime: string;
  expiryTime: string | null;
  status: BookingStatus;
}

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
