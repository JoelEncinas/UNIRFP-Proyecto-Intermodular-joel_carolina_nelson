import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Bike } from '../../../shared/domain/bike.model';
import { BookingSummary } from '../../../shared/domain/booking.model';
import { Station } from '../../../shared/domain/station.model';
import { calculateDistanceMeters } from '../../../shared/geo/distance';
import { MapApi } from '../data-access/map-api';
import { MapStore } from './map.store';

const ORIGIN = { latitude: 40.4168, longitude: -3.7038 };

describe('MapStore geolocation filters', () => {
  let store: MapStore;

  beforeEach(() => {
    const mapApiStub = {
      getStations: () => of([] as Station[]),
      getAllBikes: () => of([] as Bike[]),
      getAvailableBikes: () => of([] as Bike[]),
      getMyBookings: () => of([] as BookingSummary[]),
      getMe: () => of({ id: 2, balance: 10 }),
      getPaymentConfig: () => of({ unlockFee: 1, currency: 'eur', minTopUpAmount: 0.5 }),
      createBooking: () => of(),
      finalizeStripeUnlock: () => of(),
      cancelStripeUnlock: () => of(),
      returnBooking: () => of(),
    } as unknown as MapApi;

    TestBed.configureTestingModule({
      providers: [MapStore, { provide: MapApi, useValue: mapApiStub }],
    });
    store = TestBed.inject(MapStore);
    store.setUserLocation(ORIGIN.latitude, ORIGIN.longitude);
  });

  it('selects the nearest unlockable station within 150m', () => {
    store.stations.set([
      station(1, meterOffset(80), 3),
      station(2, meterOffset(120), 3),
    ]);
    store.availableBikes.set([
      bike(11, 'AVAILABLE', 1),
      bike(12, 'AVAILABLE', 2),
    ]);

    expect(store.nearestUnlockableStation()?.id).toBe(1);
    expect(store.canUnlock()).toBe(true);
  });

  it('returns null when all unlockable stations are farther than 150m', () => {
    store.stations.set([
      station(1, meterOffset(151), 4),
      station(2, meterOffset(220), 4),
    ]);
    store.availableBikes.set([
      bike(11, 'AVAILABLE', 1),
      bike(12, 'AVAILABLE', 2),
    ]);

    expect(store.nearestUnlockableStation()).toBeNull();
    expect(store.canUnlock()).toBe(false);
  });

  it('accepts the 150m boundary as unlockable', () => {
    store.stations.set([station(1, meterOffset(150), 2)]);
    store.availableBikes.set([bike(11, 'AVAILABLE', 1)]);

    const nearest = store.nearestUnlockableStation();
    expect(nearest).not.toBeNull();
    expect(nearest?.id).toBe(1);
    expect((nearest?.distanceMeters ?? Infinity) <= 150).toBe(true);
  });

  it('canUnlock is false when user already has ACTIVE or PENDING bookings', () => {
    store.stations.set([station(1, meterOffset(60), 3)]);
    store.availableBikes.set([bike(11, 'AVAILABLE', 1)]);
    store.bookings.set([
      booking(99, 'ACTIVE'),
      booking(100, 'PENDING'),
    ]);

    expect(store.nearestUnlockableStation()).not.toBeNull();
    expect(store.canUnlock()).toBe(false);
  });

  it('nearestReturnStation ignores full stations without free docks', () => {
    store.stations.set([
      station(1, meterOffset(40), 1),
      station(2, meterOffset(110), 2),
    ]);
    store.allBikes.set([
      bike(11, 'AVAILABLE', 1),
      bike(12, 'AVAILABLE', 2),
    ]);
    store.availableBikes.set([]);

    const nearestReturn = store.nearestReturnStation();
    expect(nearestReturn).not.toBeNull();
    expect(nearestReturn?.id).toBe(2);
  });
});

function station(id: number, latitude: number, capacity: number): Station {
  return {
    id,
    name: `Station ${id}`,
    address: `Address ${id}`,
    latitude,
    longitude: ORIGIN.longitude,
    capacity,
  };
}

function bike(id: number, status: Bike['status'], stationId: number | null): Bike {
  return {
    id,
    model: `Bike-${id}`,
    status,
    stationId,
  };
}

function booking(id: number, status: BookingSummary['status']): BookingSummary {
  return {
    id,
    userId: 2,
    username: 'rider1',
    bikeId: 1,
    bikeModel: 'Bike-A',
    pickupStationId: 2,
    pickupStationName: 'Station Open',
    dropoffStationId: null,
    dropoffStationName: null,
    startTime: '2026-01-10T09:00:00Z',
    expiryTime: null,
    activatedAt: status === 'ACTIVE' ? '2026-01-10T09:02:00Z' : null,
    returnedAt: null,
    status,
  };
}

function meterOffset(targetMeters: number): number {
  const initialLatitudeDelta = targetMeters / 111_320;
  const estimatedDistance = calculateDistanceMeters(ORIGIN, {
    latitude: ORIGIN.latitude + initialLatitudeDelta,
    longitude: ORIGIN.longitude,
  });
  const scale = targetMeters / estimatedDistance;
  return ORIGIN.latitude + initialLatitudeDelta * scale;
}
