import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { finalize, forkJoin, switchMap } from 'rxjs';

import { Bike, BikeStatus } from '../../../shared/domain/bike.model';
import { BookingStatus } from '../../../shared/domain/booking.model';
import { Station, StationWithAvailability } from '../../../shared/domain/station.model';
import { calculateDistanceMeters } from '../../../shared/geo/distance';
import { MapApi } from '../data-access/map-api';
import { MapCoordinate, MapLoadSnapshot } from '../models/map.models';

const ACTIVE_OR_PENDING_BOOKING_STATUSES: BookingStatus[] = ['PENDING', 'ACTIVE'];
const DOCK_OCCUPYING_BIKE_STATUSES: BikeStatus[] = ['AVAILABLE', 'BOOKED', 'MAINTENANCE'];
const UNLOCK_DISTANCE_METERS = 150;

@Injectable({
  providedIn: 'root',
})
export class MapStore {
  private readonly mapApi = inject(MapApi);

  readonly isLoading = signal(false);
  readonly isUnlocking = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly unlockMessage = signal<string | null>(null);
  readonly stations = signal<Station[]>([]);
  readonly allBikes = signal<Bike[]>([]);
  readonly availableBikes = signal<Bike[]>([]);
  readonly bookings = signal<MapLoadSnapshot['bookings']>([]);
  readonly userLocation = signal<MapCoordinate | null>(null);

  readonly activeBooking = computed(
    () => this.bookings().find((booking) => booking.status === 'ACTIVE') ?? null,
  );

  readonly hasActiveBooking = computed(() =>
    this.bookings().some((booking) => ACTIVE_OR_PENDING_BOOKING_STATUSES.includes(booking.status)),
  );

  readonly isReturnMode = computed(() => this.activeBooking() !== null);

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

  readonly stationOccupancy = computed(() => {
    const counts = new Map<number, number>();

    for (const bike of this.allBikes()) {
      if (bike.stationId === null || !DOCK_OCCUPYING_BIKE_STATUSES.includes(bike.status)) {
        continue;
      }
      counts.set(bike.stationId, (counts.get(bike.stationId) ?? 0) + 1);
    }

    return counts;
  });

  readonly stationsWithAvailability = computed<StationWithAvailability[]>(() => {
    const availability = this.stationAvailability();
    const occupancy = this.stationOccupancy();
    const location = this.userLocation();

    return this.stations().map((station) => {
      const availableBikes = availability.get(station.id) ?? 0;
      const occupiedDocks = occupancy.get(station.id) ?? 0;
      const availableDocks = Math.max(station.capacity - occupiedDocks, 0);
      const distanceMeters =
        location && station.latitude !== null && station.longitude !== null
          ? calculateDistanceMeters(location, {
              latitude: station.latitude,
              longitude: station.longitude,
            })
          : null;

      return {
        ...station,
        availableBikes,
        distanceMeters,
        occupiedDocks,
        availableDocks,
        hasFreeDock: availableDocks > 0,
      };
    });
  });

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

  readonly nearestReturnStation = computed<StationWithAvailability | null>(() => {
    const candidates = this.stationsWithAvailability()
      .filter((station) => station.hasFreeDock && station.distanceMeters !== null)
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

  readonly canUnlock = computed(
    () => !this.hasActiveBooking() && this.nearestUnlockableStation() !== null,
  );

  readonly canReturn = computed(
    () => this.isReturnMode() && this.nearestReturnStation() !== null,
  );

  readonly canExecutePrimaryAction = computed(() =>
    this.isReturnMode() ? this.canReturn() : this.canUnlock(),
  );

  readonly unlockButtonLabel = computed(() => {
    if (this.isReturnMode()) {
      return this.canReturn() ? 'devolver bicicleta' : 'sin estaciones cercanas con espacio';
    }

    if (this.hasActiveBooking()) {
      return 'ya tienes una bicicleta activa';
    }

    return this.canUnlock() ? 'desbloquear bicicleta' : 'sin puestos cercanos, acercate mas';
  });

  loadInitialData(): void {
    this.errorMessage.set(null);
    this.isLoading.set(true);

    this.createLoadSnapshot$()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (snapshot) => {
          this.applySnapshot(snapshot);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  unlockNearestBike(userId: number): void {
    if (this.isUnlocking() || this.isLoading()) {
      return;
    }

    this.unlockMessage.set(null);

    if (this.hasActiveBooking()) {
      this.unlockMessage.set('ya tienes una bicicleta activa');
      return;
    }

    const nearestStation = this.nearestUnlockableStation();
    if (!nearestStation) {
      this.unlockMessage.set('sin puestos cercanos, acercate mas');
      return;
    }

    const bike = this.availableBikes().find((candidate) => candidate.stationId === nearestStation.id);
    if (!bike) {
      this.unlockMessage.set('No quedan bicis disponibles en la estacion seleccionada.');
      return;
    }

    this.isUnlocking.set(true);

    this.mapApi
      .createBooking({
        userId,
        bikeId: bike.id,
        expiryTime: null,
      })
      .pipe(
        switchMap((booking) => this.mapApi.activateBooking(booking.id)),
        switchMap(() => this.createLoadSnapshot$()),
        finalize(() => this.isUnlocking.set(false)),
      )
      .subscribe({
        next: (snapshot) => {
          this.applySnapshot(snapshot);
          this.unlockMessage.set('Bicicleta desbloqueada correctamente.');
        },
        error: (error: unknown) => {
          this.unlockMessage.set(this.toErrorMessage(error, 'No se pudo desbloquear la bicicleta.'));
        },
      });
  }

  returnActiveBike(): void {
    if (this.isUnlocking() || this.isLoading()) {
      return;
    }

    this.unlockMessage.set(null);

    const booking = this.activeBooking();
    if (!booking) {
      this.unlockMessage.set('No tienes una bicicleta activa para devolver.');
      return;
    }

    const nearestStation = this.nearestReturnStation();
    if (!nearestStation || !nearestStation.hasFreeDock) {
      this.unlockMessage.set('No hay estaciones cercanas con espacio disponible.');
      return;
    }

    this.isUnlocking.set(true);

    this.mapApi
      .returnBooking(booking.id, { stationId: nearestStation.id })
      .pipe(
        switchMap(() => this.createLoadSnapshot$()),
        finalize(() => this.isUnlocking.set(false)),
      )
      .subscribe({
        next: (snapshot) => {
          this.applySnapshot(snapshot);
          this.unlockMessage.set(`Bicicleta devuelta correctamente en ${nearestStation.name}.`);
        },
        error: (error: unknown) => {
          this.unlockMessage.set(this.toErrorMessage(error, 'No se pudo devolver la bicicleta.'));
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

  clearUnlockMessage(): void {
    this.unlockMessage.set(null);
  }

  private createLoadSnapshot$() {
    return forkJoin({
      stations: this.mapApi.getStations(),
      allBikes: this.mapApi.getAllBikes(),
      availableBikes: this.mapApi.getAvailableBikes(),
      bookings: this.mapApi.getMyBookings(),
    });
  }

  private applySnapshot(snapshot: MapLoadSnapshot): void {
    this.stations.set(snapshot.stations);
    this.allBikes.set(snapshot.allBikes);
    this.availableBikes.set(snapshot.availableBikes);
    this.bookings.set(snapshot.bookings);
  }

  private toErrorMessage(error: unknown, fallback = 'No se pudieron cargar los datos del mapa.'): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    return apiMessage ?? fallback;
  }

}
