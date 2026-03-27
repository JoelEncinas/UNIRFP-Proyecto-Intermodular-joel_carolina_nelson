export type BikeStatus = 'AVAILABLE' | 'BOOKED' | 'BUSY' | 'MAINTENANCE';

export interface Bike {
  id: number;
  model: string;
  status: BikeStatus;
  stationId: number | null;
}
