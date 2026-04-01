import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Auth } from '../../../../core/auth/auth';
import {
  GEOLOCATION_REQUEST_OPTIONS,
  getLocationErrorMessage,
} from '../../../../shared/geo/location-errors';
import { MapStore } from '../../state/map.store';
import { LeafletMap } from '../../ui/leaflet-map/leaflet-map';

@Component({
  selector: 'app-map-page',
  imports: [LeafletMap],
  templateUrl: './map-page.html',
  styleUrl: './map-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapPage implements OnInit {
  private readonly auth = inject(Auth);
  private readonly route = inject(ActivatedRoute);

  readonly mapStore = inject(MapStore);
  readonly isLocating = signal(false);
  readonly locationMessage = signal<string | null>(null);
  readonly focusStationId = signal<number | null>(null);
  readonly panToUser = signal(true);

  readonly nearestStationSummary = computed(() => {
    const station = this.mapStore.isReturnMode()
      ? this.mapStore.nearestReturnStation()
      : this.mapStore.nearestUnlockableStation();

    if (!station) {
      return null;
    }

    const prefix = this.mapStore.isReturnMode() ? 'Devolucion cercana' : 'Estacion cercana';
    const availabilityText = this.mapStore.isReturnMode()
      ? `${station.availableDocks} ${station.availableDocks === 1 ? 'hueco' : 'huecos'} disponible`
      : `${station.availableBikes} ${station.availableBikes === 1 ? 'bici' : 'bicis'} disponible`;

    return `${prefix}: ${station.name} - ${this.formatDistance(station.distanceMeters)} - ${availabilityText}`;
  });

  ngOnInit(): void {
    this.mapStore.loadInitialData();
    const stationId = this.parseStationId(this.route.snapshot.queryParamMap.get('stationId'));
    this.focusStationId.set(stationId);
    this.requestUserLocation(stationId === null);
  }

  requestUserLocation(allowMapPan = true): void {
    const geolocation = globalThis.navigator?.geolocation;
    this.mapStore.clearUnlockMessage();
    this.panToUser.set(allowMapPan);

    if (!geolocation) {
      this.locationMessage.set('Este dispositivo no soporta geolocalizacion.');
      return;
    }

    this.isLocating.set(true);
    this.locationMessage.set('Solicitando permiso de ubicacion...');

    geolocation.getCurrentPosition(
      (position) => {
        this.mapStore.setUserLocation(position.coords.latitude, position.coords.longitude);
        this.locationMessage.set('Ubicacion detectada correctamente.');
        this.isLocating.set(false);
      },
      (error) => {
        this.locationMessage.set(getLocationErrorMessage(error.code, 'map'));
        this.isLocating.set(false);
      },
      GEOLOCATION_REQUEST_OPTIONS,
    );
  }

  onUnlockClick(): void {
    if (
      this.mapStore.isLoading() ||
      this.mapStore.isUnlocking() ||
      !this.mapStore.canExecutePrimaryAction()
    ) {
      return;
    }

    if (this.mapStore.isReturnMode()) {
      this.mapStore.returnActiveBike();
      return;
    }

    const userId = this.auth.getAuthenticatedUserId();
    if (userId === null) {
      this.locationMessage.set('Sesion no valida. Inicia sesion de nuevo.');
      return;
    }

    this.mapStore.unlockNearestBike(userId);
  }

  private formatDistance(distanceMeters: number | null): string {
    if (distanceMeters === null) {
      return 'distancia no disponible';
    }

    return `${Math.round(distanceMeters)} m`;
  }

  private parseStationId(value: string | null): number | null {
    if (value === null) {
      return null;
    }

    const parsed = Number(value);
    if (!Number.isInteger(parsed) || parsed <= 0) {
      return null;
    }

    return parsed;
  }
}
