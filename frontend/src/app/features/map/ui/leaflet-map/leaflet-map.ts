import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  input,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';

import { MapCoordinate } from '../../models/map.models';
import { StationWithAvailability } from '../../models/station.model';

const DEFAULT_MAP_ZOOM = 12;
const STATION_FOCUS_ZOOM = 14;
const FIT_BOUNDS_MAX_ZOOM = 14;

@Component({
  selector: 'app-leaflet-map',
  imports: [],
  templateUrl: './leaflet-map.html',
  styleUrl: './leaflet-map.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeafletMap implements AfterViewInit, OnDestroy {
  private readonly mapElement = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');

  readonly stations = input<StationWithAvailability[]>([]);
  readonly userLocation = input<MapCoordinate | null>(null);
  readonly isLoading = input(false);
  readonly focusStationId = input<number | null>(null);
  readonly panToUser = input(true);

  readonly hasRenderableStations = computed(() =>
    this.stations().some((station) => station.latitude !== null && station.longitude !== null),
  );

  private map: L.Map | null = null;
  private readonly stationsLayer = L.layerGroup();
  private readonly userLayer = L.layerGroup();
  private resizeObserver: ResizeObserver | null = null;
  private hasInitializedView = false;
  private lastFocusedStationId: number | null = null;
  private pendingInvalidateTimeouts: number[] = [];
  private readonly onWindowResize = () => this.scheduleInvalidateSize([0, 120, 320, 700]);
  private readonly onVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      this.scheduleInvalidateSize([0, 120, 320, 700]);
    }
  };

  constructor() {
    effect(() => {
      this.renderLayers(
        this.stations(),
        this.userLocation(),
        this.focusStationId(),
        this.panToUser(),
      );
    });
  }

  ngAfterViewInit(): void {
    const mapContainer = this.mapElement().nativeElement;
    this.map = L.map(mapContainer, {
      zoomControl: true,
      attributionControl: true,
    }).setView([40.416775, -3.70379], DEFAULT_MAP_ZOOM);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.stationsLayer.addTo(this.map);
    this.userLayer.addTo(this.map);
    this.scheduleInvalidateSize([0, 120, 320, 700]);

    if ('ResizeObserver' in globalThis) {
      this.resizeObserver = new ResizeObserver(() => {
        this.scheduleInvalidateSize([0, 80, 240]);
      });
      this.resizeObserver.observe(mapContainer);
    }

    globalThis.addEventListener('resize', this.onWindowResize);
    globalThis.addEventListener('orientationchange', this.onWindowResize);
    document.addEventListener('visibilitychange', this.onVisibilityChange);

    this.renderLayers(
      this.stations(),
      this.userLocation(),
      this.focusStationId(),
      this.panToUser(),
    );
  }

  ngOnDestroy(): void {
    globalThis.removeEventListener('resize', this.onWindowResize);
    globalThis.removeEventListener('orientationchange', this.onWindowResize);
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.clearPendingInvalidations();
    this.map?.remove();
    this.map = null;
  }

  private renderLayers(
    stations: StationWithAvailability[],
    userLocation: MapCoordinate | null,
    focusStationId: number | null,
    panToUser: boolean,
  ): void {
    if (!this.map) {
      return;
    }

    this.stationsLayer.clearLayers();
    this.userLayer.clearLayers();

    const bounds = L.latLngBounds([]);
    const stationCoordinatesById = new Map<number, L.LatLng>();

    for (const station of stations) {
      if (station.latitude === null || station.longitude === null) {
        continue;
      }

      const latLng = L.latLng(station.latitude, station.longitude);
      bounds.extend(latLng);
      stationCoordinatesById.set(station.id, latLng);

      const available = station.availableBikes;
      const tooltipLabel = this.buildStationTooltipLabel(station.name, available, station.capacity);

      L.marker(latLng, {
        icon: this.buildStationMarkerIcon(available, station.capacity),
      })
        .bindTooltip(tooltipLabel, {
          direction: 'top',
          offset: [0, -16],
          opacity: 1,
          className: 'map-station-tooltip',
        })
        .addTo(this.stationsLayer);
    }

    if (focusStationId === null) {
      this.lastFocusedStationId = null;
    } else if (focusStationId !== this.lastFocusedStationId) {
      const focusCoordinates = stationCoordinatesById.get(focusStationId);

      if (focusCoordinates) {
        const currentZoom = this.map.getZoom();
        const targetZoom = Number.isFinite(currentZoom)
          ? Math.max(currentZoom, STATION_FOCUS_ZOOM)
          : STATION_FOCUS_ZOOM;
        this.map.setView(focusCoordinates, targetZoom);
        this.lastFocusedStationId = focusStationId;
        this.hasInitializedView = true;
      }
    }

    if (userLocation) {
      const latLng = L.latLng(userLocation.latitude, userLocation.longitude);
      bounds.extend(latLng);

      L.marker(latLng, {
        icon: this.buildUserMarkerIcon(),
        interactive: false,
        zIndexOffset: 2000,
      })
        .addTo(this.userLayer);

      if (panToUser) {
        this.map.panTo(latLng);
      }
    }

    if (!this.hasInitializedView && bounds.isValid()) {
      this.map.fitBounds(bounds.pad(0.2), { maxZoom: FIT_BOUNDS_MAX_ZOOM });
      this.hasInitializedView = true;
      this.scheduleInvalidateSize([0, 120, 320]);
    }
  }

  private buildStationTooltipLabel(name: string, availableBikes: number, capacity: number): string {
    const bikeLabel = availableBikes === 1 ? 'bici' : 'bicis';
    const safeCapacity = Math.max(capacity, 0);
    return `${this.escapeHtml(name)} · ${availableBikes}/${safeCapacity} ${bikeLabel}`;
  }

  private buildStationMarkerIcon(availableBikes: number, capacity: number): L.DivIcon {
    const colors = this.resolveAvailabilityColors(availableBikes, capacity);
    return L.divIcon({
      className: 'map-station-marker-host',
      iconSize: [38, 38],
      iconAnchor: [19, 19],
      tooltipAnchor: [0, -20],
      html: `
        <span
          class="map-station-marker__bubble"
          style="--station-stroke: ${colors.stroke}; --station-fill: ${colors.fill};"
        >
          ${availableBikes}
        </span>
      `,
    });
  }

  private buildUserMarkerIcon(): L.DivIcon {
    return L.divIcon({
      className: 'map-user-marker-host',
      iconSize: [36, 36],
      iconAnchor: [18, 18],
      html: `
        <span class="map-user-marker">
          <span class="map-user-marker__pulse"></span>
          <span class="map-user-marker__core"></span>
          <span class="map-user-marker__ring"></span>
        </span>
      `,
    });
  }

  private resolveAvailabilityColors(
    availableBikes: number,
    capacity: number,
  ): { stroke: string; fill: string } {
    if (availableBikes <= 0) {
      return { stroke: '#94a3b8', fill: '#e2e8f0' };
    }

    if (capacity <= 0) {
      return { stroke: '#e11d48', fill: '#fda4af' };
    }

    const availabilityRatio = (availableBikes / capacity) * 100;
    if (availabilityRatio > 66) {
      return { stroke: '#059669', fill: '#6ee7b7' };
    }

    if (availabilityRatio >= 33) {
      return { stroke: '#d97706', fill: '#fcd34d' };
    }

    return { stroke: '#e11d48', fill: '#fda4af' };
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  private scheduleInvalidateSize(delaysMs: number[]): void {
    if (!this.map) {
      return;
    }

    this.clearPendingInvalidations();

    for (const delayMs of delaysMs) {
      const timeoutId = globalThis.setTimeout(() => {
        if (!this.map) {
          return;
        }

        const mapContainer = this.mapElement().nativeElement;
        if (mapContainer.clientWidth === 0 || mapContainer.clientHeight === 0) {
          return;
        }

        this.map.invalidateSize();
      }, delayMs);

      this.pendingInvalidateTimeouts.push(timeoutId);
    }
  }

  private clearPendingInvalidations(): void {
    for (const timeoutId of this.pendingInvalidateTimeouts) {
      globalThis.clearTimeout(timeoutId);
    }
    this.pendingInvalidateTimeouts = [];
  }
}
