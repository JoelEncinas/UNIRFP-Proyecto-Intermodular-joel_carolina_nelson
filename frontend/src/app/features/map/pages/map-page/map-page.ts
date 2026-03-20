import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';

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
  readonly mapStore = inject(MapStore);
  readonly isLocating = signal(false);
  readonly locationMessage = signal<string | null>(null);
  readonly unlockMessage = signal<string | null>(null);

  readonly nearestStationSummary = computed(() => {
    const station = this.mapStore.nearestUnlockableStation();
    if (!station) {
      return null;
    }

    return `${station.name} · ${this.formatDistance(station.distanceMeters)} · ${station.availableBikes} bicis`;
  });

  ngOnInit(): void {
    this.mapStore.loadInitialData();
    this.requestUserLocation();
  }

  requestUserLocation(): void {
    const geolocation = globalThis.navigator?.geolocation;
    this.unlockMessage.set(null);

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
        this.locationMessage.set(this.toLocationErrorMessage(error.code));
        this.isLocating.set(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      },
    );
  }

  onUnlockClick(): void {
    if (!this.mapStore.isUnlockEnabled() || this.mapStore.isLoading()) {
      return;
    }

    this.unlockMessage.set('Siguiente paso: conectar desbloqueo y persistencia.');
  }

  private formatDistance(distanceMeters: number | null): string {
    if (distanceMeters === null) {
      return 'distancia no disponible';
    }

    return `${Math.round(distanceMeters)} m`;
  }

  private toLocationErrorMessage(errorCode: number): string {
    switch (errorCode) {
      case 1:
        return 'Permiso de ubicacion denegado. Activalo para desbloquear cerca de estaciones.';
      case 2:
        return 'No se pudo obtener tu ubicacion actual. Intentalo de nuevo.';
      case 3:
        return 'La solicitud de ubicacion tardo demasiado.';
      default:
        return 'No se pudo obtener tu ubicacion.';
    }
  }
}
