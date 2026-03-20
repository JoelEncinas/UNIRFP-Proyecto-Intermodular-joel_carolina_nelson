import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, AfterViewInit, computed, effect, input, viewChild } from '@angular/core';
import * as L from 'leaflet';

import { MapCoordinate } from '../../models/map.models';
import { StationWithAvailability } from '../../models/station.model';

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

  readonly hasRenderableStations = computed(() =>
    this.stations().some((station) => station.latitude !== null && station.longitude !== null),
  );

  private map: L.Map | null = null;
  private readonly stationsLayer = L.layerGroup();
  private readonly userLayer = L.layerGroup();
  private resizeObserver: ResizeObserver | null = null;
  private hasInitializedView = false;

  constructor() {
    effect(() => {
      this.renderLayers(this.stations(), this.userLocation());
    });
  }

  ngAfterViewInit(): void {
    const mapContainer = this.mapElement().nativeElement;
    this.map = L.map(mapContainer, {
      zoomControl: true,
      attributionControl: true,
    }).setView([40.416775, -3.70379], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.stationsLayer.addTo(this.map);
    this.userLayer.addTo(this.map);
    this.map.invalidateSize();
    setTimeout(() => this.map?.invalidateSize(), 0);
    setTimeout(() => this.map?.invalidateSize(), 250);

    if ('ResizeObserver' in globalThis) {
      this.resizeObserver = new ResizeObserver(() => {
        this.map?.invalidateSize();
      });
      this.resizeObserver.observe(mapContainer);
    }

    this.renderLayers(this.stations(), this.userLocation());
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.map?.remove();
    this.map = null;
  }

  private renderLayers(stations: StationWithAvailability[], userLocation: MapCoordinate | null): void {
    if (!this.map) {
      return;
    }

    this.stationsLayer.clearLayers();
    this.userLayer.clearLayers();

    const bounds = L.latLngBounds([]);

    for (const station of stations) {
      if (station.latitude === null || station.longitude === null) {
        continue;
      }

      const latLng = L.latLng(station.latitude, station.longitude);
      bounds.extend(latLng);

      const available = station.availableBikes;
      const hasAvailability = available > 0;

      L.circleMarker(latLng, {
        radius: 8,
        color: hasAvailability ? '#0284c7' : '#64748b',
        weight: 2,
        fillColor: hasAvailability ? '#0ea5e9' : '#94a3b8',
        fillOpacity: 0.9,
      })
        .bindPopup(
          `<strong>${this.escapeHtml(station.name)}</strong><br/>Bicis disponibles: ${available}<br/>Capacidad: ${station.capacity}`,
        )
        .addTo(this.stationsLayer);
    }

    if (userLocation) {
      const latLng = L.latLng(userLocation.latitude, userLocation.longitude);
      bounds.extend(latLng);

      L.circleMarker(latLng, {
        radius: 9,
        color: '#0f172a',
        weight: 2,
        fillColor: '#22d3ee',
        fillOpacity: 0.95,
      })
        .bindPopup('Tu ubicacion')
        .addTo(this.userLayer);

      this.map.panTo(latLng);
    }

    if (!this.hasInitializedView && bounds.isValid()) {
      this.map.fitBounds(bounds.pad(0.2));
      this.hasInitializedView = true;
    }
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
}
