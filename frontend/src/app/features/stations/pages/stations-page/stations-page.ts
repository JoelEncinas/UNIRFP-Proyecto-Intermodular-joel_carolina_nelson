import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Bike } from '../../../map/models/bike.model';
import { MapCoordinate } from '../../../map/models/map.models';
import { Station } from '../../../map/models/station.model';
import { StationsApi } from '../../data-access/stations-api';

interface StationListItem extends Station {
  availableBikes: number;
  distanceMeters: number | null;
}

const PROXIMITY_RADIUS_METERS = 1000;

@Component({
  selector: 'app-stations-page',
  imports: [CommonModule],
  templateUrl: './stations-page.html',
  styleUrl: './stations-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StationsPage implements OnInit {
  private readonly stationsApi = inject(StationsApi);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);
  readonly isLocating = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly locationMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly proximityFilterEnabled = signal(false);
  readonly userLocation = signal<MapCoordinate | null>(null);
  readonly stations = signal<Station[]>([]);
  readonly availableBikes = signal<Bike[]>([]);

  readonly stationsWithDetails = computed<StationListItem[]>(() => {
    const countsByStation = this.toAvailableBikeCountByStation(this.availableBikes());
    const location = this.userLocation();

    return this.stations().map((station) => ({
      ...station,
      availableBikes: countsByStation.get(station.id) ?? 0,
      distanceMeters:
        location && station.latitude !== null && station.longitude !== null
          ? this.calculateDistanceMeters(location, {
              latitude: station.latitude,
              longitude: station.longitude,
            })
          : null,
    }));
  });

  readonly filteredStations = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    let entries = this.stationsWithDetails().filter((station) =>
      station.name.toLowerCase().includes(term),
    );

    if (this.proximityFilterEnabled()) {
      entries = entries
        .filter(
          (station) =>
            station.distanceMeters !== null && station.distanceMeters <= PROXIMITY_RADIUS_METERS,
        )
        .sort((a, b) => (a.distanceMeters ?? Infinity) - (b.distanceMeters ?? Infinity));
    } else {
      entries = [...entries].sort((a, b) => a.name.localeCompare(b.name));
    }

    return entries;
  });

  ngOnInit(): void {
    this.loadStations();
    this.requestUserLocation();
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.searchTerm.set(target?.value ?? '');
  }

  toggleProximityFilter(): void {
    if (this.proximityFilterEnabled()) {
      this.proximityFilterEnabled.set(false);
      this.locationMessage.set('Filtro por cercania desactivado.');
      return;
    }

    if (this.userLocation()) {
      this.proximityFilterEnabled.set(true);
      this.locationMessage.set('Filtro por cercania activado.');
      return;
    }

    this.requestUserLocation(true);
  }

  requestUserLocation(activateFilter = false): void {
    const geolocation = globalThis.navigator?.geolocation;

    if (!geolocation) {
      this.locationMessage.set('Este dispositivo no soporta geolocalizacion.');
      return;
    }

    this.isLocating.set(true);
    this.locationMessage.set('Detectando ubicacion...');

    geolocation.getCurrentPosition(
      (position) => {
        this.userLocation.set({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });

        if (activateFilter) {
          this.proximityFilterEnabled.set(true);
          this.locationMessage.set('Filtro por cercania activado.');
        } else {
          this.locationMessage.set('Ubicacion actualizada.');
        }

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

  formatDistance(distanceMeters: number | null): string {
    if (distanceMeters === null) {
      return 'Sin distancia';
    }

    const roundedMeters = Math.round(distanceMeters);
    if (roundedMeters <= 999) {
      return `${roundedMeters} m`;
    }

    const kilometers = (roundedMeters / 1000).toFixed(1).replace('.', ',');
    return `${kilometers}km`;
  }

  availabilityBadgeClass(station: Pick<StationListItem, 'availableBikes' | 'capacity'>): string {
    if (station.availableBikes <= 0) {
      return 'border-slate-300 bg-slate-100 text-slate-700';
    }

    if (station.capacity <= 0) {
      return 'border-rose-200 bg-rose-50 text-rose-700';
    }

    const availabilityRatio = (station.availableBikes / station.capacity) * 100;
    if (availabilityRatio > 66) {
      return 'border-emerald-200 bg-emerald-50 text-emerald-700';
    }

    if (availabilityRatio >= 33) {
      return 'border-amber-200 bg-amber-50 text-amber-700';
    }

    return 'border-rose-200 bg-rose-50 text-rose-700';
  }

  canOpenOnMap(station: Pick<Station, 'latitude' | 'longitude'>): boolean {
    return station.latitude !== null && station.longitude !== null;
  }

  openStationOnMap(stationId: number, canOpen: boolean): void {
    if (!canOpen) {
      return;
    }

    void this.router.navigate(['/app/map'], {
      queryParams: { stationId },
    });
  }

  onStationKeydown(event: KeyboardEvent, stationId: number, canOpen: boolean): void {
    if (!canOpen) {
      return;
    }

    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    this.openStationOnMap(stationId, canOpen);
  }

  private loadStations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      stations: this.stationsApi.getStations(),
      availableBikes: this.stationsApi.getAvailableBikes(),
    }).subscribe({
      next: ({ stations, availableBikes }) => {
        this.stations.set(stations);
        this.availableBikes.set(availableBikes);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.toErrorMessage(error));
        this.isLoading.set(false);
      },
    });
  }

  private toAvailableBikeCountByStation(bikes: Bike[]): Map<number, number> {
    const counts = new Map<number, number>();

    for (const bike of bikes) {
      if (bike.stationId === null) {
        continue;
      }

      counts.set(bike.stationId, (counts.get(bike.stationId) ?? 0) + 1);
    }

    return counts;
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

  private toLocationErrorMessage(errorCode: number): string {
    switch (errorCode) {
      case 1:
        return 'Permiso de ubicacion denegado.';
      case 2:
        return 'No se pudo obtener la ubicacion actual.';
      case 3:
        return 'La solicitud de ubicacion tardo demasiado.';
      default:
        return 'No se pudo obtener tu ubicacion.';
    }
  }

  private toErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudieron cargar las estaciones.';
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    return apiMessage ?? 'No se pudieron cargar las estaciones.';
  }
}
