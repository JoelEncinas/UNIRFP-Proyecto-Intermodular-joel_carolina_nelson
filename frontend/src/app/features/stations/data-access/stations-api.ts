import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { buildApiUrl } from '../../../core/http/api-url';
import { Bike } from '../../map/models/bike.model';
import { Station } from '../../map/models/station.model';

@Injectable({
  providedIn: 'root',
})
export class StationsApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(buildApiUrl(this.apiBaseUrl, '/api/stations'));
  }

  getAvailableBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(buildApiUrl(this.apiBaseUrl, '/api/bikes?status=AVAILABLE'));
  }
}
