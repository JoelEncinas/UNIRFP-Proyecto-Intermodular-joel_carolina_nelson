import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { finalize, forkJoin } from 'rxjs';

import { MapApi } from '../data-access/map-api';
import { Bike } from '../models/bike.model';
import { BookingStatus, MapCoordinate, MapLoadSnapshot } from '../models/map.models';
import { Station, StationWithAvailability } from '../models/station.model';

const ACTIVE_BOOKING_STATUSES: BookingStatus[] = ['PENDING', 'ACTIVE'];
const UNLOCK_DISTANCE_METERS = 150;

@Injectable({
  providedIn: 'root',
})
export class MapStore {
  private readonly mapApi = inject(MapApi);

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly stations = signal<Station[]>([]);
  readonly availableBikes = signal<Bike[]>([]);
  readonly bookings = signal<MapLoadSnapshot['bookings']>([]);
  readonly userLocation = signal<MapCoordinate | null>(null);

  readonly stationAvailability = computed(() => {
    const counts = new Map<number, number>();
    for (const bike of this.availableBikes()) {
      if (bike.stationId === null) {
        continue;
      }
      counts.set(bike.stationId, (counts.get(bike.stationId) ?? 0) + 1);
    }
    return counts;
  });

  readonly stationsWithAvailability = computed<StationWithAvailability[]>(() => {
    const availability = this.stationAvailability();
    const location = this.userLocation();

    return this.stations().map((station) => {
      const availableBikes = availability.get(station.id) ?? 0;
      const distanceMeters =
        location && station.latitude !== null && station.longitude !== null
          ? this.calculateDistanceMeters(location, {
              latitude: station.latitude,
              longitude: station.longitude,
            })
          : null;

      return {
        ...station,
        availableBikes,
        distanceMeters,
      };
    });
  });

  readonly hasActiveBooking = computed(() =>
    this.bookings().some((booking) => ACTIVE_BOOKING_STATUSES.includes(booking.status)),
  );

  readonly nearestUnlockableStation = computed<StationWithAvailability | null>(() => {
    const candidates = this.stationsWithAvailability()
      .filter((station) => station.availableBikes > 0 && station.distanceMeters !== null)
      .sort((a, b) => (a.distanceMeters ?? Infinity) - (b.distanceMeters ?? Infinity));

    if (candidates.length === 0) {
      return null;
    }

    const nearest = candidates[0];
    if ((nearest.distanceMeters ?? Infinity) > UNLOCK_DISTANCE_METERS) {
      return null;
    }

    return nearest;
  });

  readonly isUnlockEnabled = computed(
    () => !this.hasActiveBooking() && this.nearestUnlockableStation() !== null,
  );

  readonly unlockButtonLabel = computed(() => {
    if (this.hasActiveBooking()) {
      return 'ya tienes una bicicleta activa';
    }

    return this.isUnlockEnabled() ? 'desbloquear bicicleta' : 'sin puestos cercanos, acercate mas';
  });

  loadInitialData(): void {
    this.errorMessage.set(null);
    this.isLoading.set(true);

    forkJoin({
      stations: this.mapApi.getStations(),
      availableBikes: this.mapApi.getAvailableBikes(),
      bookings: this.mapApi.getMyBookings(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (snapshot) => {
          this.stations.set(snapshot.stations);
          this.availableBikes.set(snapshot.availableBikes);
          this.bookings.set(snapshot.bookings);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  refreshAvailability(): void {
    this.loadInitialData();
  }

  setUserLocation(latitude: number, longitude: number): void {
    this.userLocation.set({ latitude, longitude });
  }

  clearErrorMessage(): void {
    this.errorMessage.set(null);
  }

  private toErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudieron cargar los datos del mapa.';
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    return apiMessage ?? 'No se pudieron cargar los datos del mapa.';
  }

  private calculateDistanceMeters(from: MapCoordinate, to: MapCoordinate): number {
    const earthRadiusMeters = 6371000;
    const latitudeDelta = this.toRadians(to.latitude - from.latitude);
    const longitudeDelta = this.toRadians(to.longitude - from.longitude);
    const fromLatitude = this.toRadians(from.latitude);
    const toLatitude = this.toRadians(to.latitude);

    const haversine =
      Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2) +
      Math.cos(fromLatitude) *
        Math.cos(toLatitude) *
        Math.sin(longitudeDelta / 2) *
        Math.sin(longitudeDelta / 2);

    const angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return earthRadiusMeters * angularDistance;
  }

  private toRadians(value: number): number {
    return (value * Math.PI) / 180;
  }
}
