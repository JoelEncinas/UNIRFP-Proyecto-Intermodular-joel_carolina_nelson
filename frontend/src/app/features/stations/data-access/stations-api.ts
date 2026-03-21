import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { Bike } from '../../map/models/bike.model';
import { Station } from '../../map/models/station.model';

@Injectable({
  providedIn: 'root',
})
export class StationsApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(this.buildUrl('/api/stations'));
  }

  getAvailableBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(this.buildUrl('/api/bikes?status=AVAILABLE'));
  }

  private buildUrl(path: '/api/stations' | '/api/bikes?status=AVAILABLE'): string {
    if (!this.apiBaseUrl) {
      return path;
    }

    return `${this.apiBaseUrl.replace(/\/+$/, '')}${path}`;
  }
}
