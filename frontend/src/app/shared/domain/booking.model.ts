export type BookingStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface BookingSummary {
  id: number;
  userId: number;
  username: string;
  bikeId: number;
  bikeModel: string;
  pickupStationId: number | null;
  pickupStationName: string | null;
  dropoffStationId: number | null;
  dropoffStationName: string | null;
  startTime: string;
  expiryTime: string | null;
  activatedAt: string | null;
  returnedAt: string | null;
  status: BookingStatus;
}
