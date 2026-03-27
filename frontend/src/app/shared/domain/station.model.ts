export interface Station {
  id: number;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  capacity: number;
}

export interface StationWithAvailability extends Station {
  availableBikes: number;
  distanceMeters: number | null;
  occupiedDocks: number;
  availableDocks: number;
  hasFreeDock: boolean;
}
