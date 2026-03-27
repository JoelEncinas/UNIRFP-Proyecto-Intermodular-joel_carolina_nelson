import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Station } from '../../../../shared/domain/station.model';
import { StationsStore } from '../../state/stations.store';

@Component({
  selector: 'app-stations-page',
  imports: [CommonModule],
  providers: [StationsStore],
  templateUrl: './stations-page.html',
  styleUrl: './stations-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StationsPage implements OnInit {
  private readonly stationsStore = inject(StationsStore);

  readonly isLoading = this.stationsStore.isLoading;
  readonly isLocating = this.stationsStore.isLocating;
  readonly errorMessage = this.stationsStore.errorMessage;
  readonly locationMessage = this.stationsStore.locationMessage;
  readonly searchTerm = this.stationsStore.searchTerm;
  readonly proximityFilterEnabled = this.stationsStore.proximityFilterEnabled;
  readonly stations = this.stationsStore.stations;
  readonly filteredStations = this.stationsStore.filteredStations;

  ngOnInit(): void {
    this.stationsStore.loadInitialData();
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.stationsStore.setSearchTerm(target?.value ?? '');
  }

  toggleProximityFilter(): void {
    this.stationsStore.toggleProximityFilter();
  }

  requestUserLocation(activateFilter = false): void {
    this.stationsStore.requestUserLocation(activateFilter);
  }

  formatDistance(distanceMeters: number | null): string {
    return this.stationsStore.formatDistance(distanceMeters);
  }

  availabilityBadgeClass(station: Pick<Station, 'capacity'> & { availableBikes: number }): string {
    return this.stationsStore.availabilityBadgeClass(station);
  }

  canOpenOnMap(station: Pick<Station, 'latitude' | 'longitude'>): boolean {
    return this.stationsStore.canOpenOnMap(station);
  }

  openStationOnMap(stationId: number, canOpen: boolean): void {
    this.stationsStore.openStationOnMap(stationId, canOpen);
  }

  onStationKeydown(event: KeyboardEvent, stationId: number, canOpen: boolean): void {
    this.stationsStore.onStationKeydown(event, stationId, canOpen);
  }
}
