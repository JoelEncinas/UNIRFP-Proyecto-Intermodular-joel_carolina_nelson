import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { finalize, forkJoin, switchMap } from 'rxjs';

import { Bike, BikeStatus } from '../../../shared/domain/bike.model';
import { BookingStatus } from '../../../shared/domain/booking.model';
import { Station, StationWithAvailability } from '../../../shared/domain/station.model';
import { calculateDistanceMeters } from '../../../shared/geo/distance';
import { MapApi } from '../data-access/map-api';
import { MapCoordinate, MapLoadSnapshot, MapStripePaymentReturnState } from '../models/map.models';

const ACTIVE_OR_PENDING_BOOKING_STATUSES: BookingStatus[] = ['PENDING', 'ACTIVE'];
const DOCK_OCCUPYING_BIKE_STATUSES: BikeStatus[] = ['AVAILABLE', 'BOOKED', 'MAINTENANCE'];
const UNLOCK_DISTANCE_METERS = 150;
const STRIPE_PENDING_UNLOCK_STORAGE_KEY = 'bikeshare.pendingUnlockStripeSession';

type PendingStripeUnlockSession = {
  bookingId: number;
  sessionId: string;
};

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
  readonly balance = signal(0);
  readonly unlockFee = signal(0);
  readonly paymentCurrency = signal('EUR');
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
  
  readonly hasEnoughBalance = computed(() => this.balance() >= this.unlockFee() && this.unlockFee() > 0);
  readonly canUnlockWithSaldo = computed(() => this.canUnlock() && this.hasEnoughBalance());
  readonly walletDisabledMessage = computed(() => {
    if (this.isReturnMode() || this.hasEnoughBalance()) {
      return null;
    }
    return `Saldo insuficiente. Necesitas ${this.unlockFee().toFixed(2)} ${this.paymentCurrency()} para desbloquear.`;
  });

  readonly canReturn = computed(
    () => this.isReturnMode() && this.nearestReturnStation() !== null,
  );

  readonly canExecutePrimaryAction = computed(() =>
    this.isReturnMode() ? this.canReturn() : this.canUnlock(),
  );

  readonly unlockButtonLabel = computed(() => {
    if (this.isReturnMode()) {
      return this.canReturn() ? 'Devolver bicicleta' : 'sin estaciones cercanas con espacio';
    }

    if (this.hasActiveBooking()) {
      return 'ya tienes una bicicleta activa';
    }

    return this.canUnlock() ? 'Desbloquear bicicleta' : 'sin puestos cercanos, acercate mas';
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

  unlockNearestBikeWithSaldo(userId: number): void {
    if (this.isUnlocking() || this.isLoading()) {
      return;
    }

    this.unlockMessage.set(null);

    if (this.hasActiveBooking()) {
      this.unlockMessage.set('ya tienes una bicicleta activa');
      return;
    }
    if (!this.hasEnoughBalance()) {
      this.unlockMessage.set(
        `Saldo insuficiente. Necesitas ${this.unlockFee().toFixed(2)} ${this.paymentCurrency()} para desbloquear.`,
      );
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
        paymentMethod: 'SALDO',
      })
      .pipe(
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

  handleStripeReturn(paymentState: MapStripePaymentReturnState): void {
    if (paymentState === null) {
      this.loadInitialData();
      return;
    }

    const pendingStripeSession = this.readPendingStripeUnlockSession();
    if (pendingStripeSession === null) {
      this.loadInitialData();
      this.unlockMessage.set('No se encontro una reserva Stripe pendiente para procesar.');
      return;
    }

    this.errorMessage.set(null);
    this.isLoading.set(true);
    this.unlockMessage.set(
      paymentState === 'success' ? 'Confirmando pago y desbloqueo...' : 'Cancelando reserva pendiente...',
    );

    const action$ =
      paymentState === 'success'
        ? this.mapApi.finalizeStripeUnlock(pendingStripeSession.bookingId, pendingStripeSession)
        : this.mapApi.cancelStripeUnlock(pendingStripeSession.bookingId, pendingStripeSession);

    action$
      .pipe(
        switchMap(() => this.createLoadSnapshot$()),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (snapshot) => {
          this.applySnapshot(snapshot);
          this.clearPendingStripeUnlockSession();
          this.unlockMessage.set(
            paymentState === 'success'
              ? 'Pago confirmado. Bicicleta desbloqueada correctamente.'
              : 'Pago cancelado. Reserva liberada correctamente.',
          );
        },
        error: (error: unknown) => {
          this.unlockMessage.set(
            this.toErrorMessage(
              error,
              paymentState === 'success'
                ? 'No se pudo confirmar el desbloqueo con Stripe.'
                : 'No se pudo cancelar la reserva Stripe.',
            ),
          );
          this.loadInitialData();
        },
      });
  }

  unlockNearestBikeWithStripe(userId: number): void {
    if (this.isUnlocking() || this.isLoading()) {
      return;
    }
    this.unlockMessage.set(null);

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
        paymentMethod: 'STRIPE',
      })
      .pipe(finalize(() => this.isUnlocking.set(false)))
      .subscribe({
        next: (response) => {
          const sessionId = response.stripe?.sessionId;
          const checkoutUrl = response.stripe?.checkoutUrl;
          const bookingId = response.booking.id;
          if (!sessionId || !checkoutUrl) {
            this.unlockMessage.set('No se pudo iniciar el pago con Stripe.');
            return;
          }

          this.savePendingStripeUnlockSession({ bookingId, sessionId });

          if (typeof window !== 'undefined') {
            window.location.assign(checkoutUrl);
          }
        },
        error: (error: unknown) => {
          this.unlockMessage.set(this.toErrorMessage(error, 'No se pudo iniciar el desbloqueo con Stripe.'));
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
      me: this.mapApi.getMe(),
      paymentConfig: this.mapApi.getPaymentConfig(),
    });
  }

  private applySnapshot(snapshot: MapLoadSnapshot): void {
    this.stations.set(snapshot.stations);
    this.allBikes.set(snapshot.allBikes);
    this.availableBikes.set(snapshot.availableBikes);
    this.bookings.set(snapshot.bookings);
    this.balance.set(snapshot.me.balance);
    this.unlockFee.set(snapshot.paymentConfig.unlockFee);
    this.paymentCurrency.set(snapshot.paymentConfig.currency.toUpperCase());
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

  private savePendingStripeUnlockSession(payload: PendingStripeUnlockSession): void {
    const normalizedSessionId = payload.sessionId.trim();
    if (!normalizedSessionId || !Number.isInteger(payload.bookingId) || payload.bookingId <= 0) {
      return;
    }

    this.withSessionStorage((storage) => {
      storage.setItem(
        STRIPE_PENDING_UNLOCK_STORAGE_KEY,
        JSON.stringify({ bookingId: payload.bookingId, sessionId: normalizedSessionId }),
      );
    });
  }

  private readPendingStripeUnlockSession(): PendingStripeUnlockSession | null {
    let rawSessionData: string | null = null;
    this.withSessionStorage((storage) => {
      rawSessionData = storage.getItem(STRIPE_PENDING_UNLOCK_STORAGE_KEY);
    });

    if (!rawSessionData) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawSessionData) as Partial<PendingStripeUnlockSession>;
      const bookingId = parsed.bookingId;
      const sessionId = parsed.sessionId;
      if (
        typeof sessionId !== 'string' ||
        !sessionId.trim() ||
        typeof bookingId !== 'number' ||
        !Number.isInteger(bookingId) ||
        bookingId <= 0
      ) {
        return null;
      }
      return { bookingId, sessionId: sessionId.trim() };
    } catch {
      return null;
    }
  }

  private clearPendingStripeUnlockSession(): void {
    this.withSessionStorage((storage) => {
      storage.removeItem(STRIPE_PENDING_UNLOCK_STORAGE_KEY);
    });
  }

  private withSessionStorage(action: (storage: Storage) => void): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      action(window.sessionStorage);
    } catch {
      // Ignore browser storage failures (privacy mode, blocked storage, etc.)
    }
  }

}
