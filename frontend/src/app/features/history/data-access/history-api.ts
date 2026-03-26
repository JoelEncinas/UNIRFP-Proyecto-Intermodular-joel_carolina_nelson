import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { buildApiUrl } from '../../../core/http/api-url';
import { BookingSummary } from '../../map/models/map.models';

@Injectable({
  providedIn: 'root',
})
export class HistoryApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getMyBookings(): Observable<BookingSummary[]> {
    return this.http.get<BookingSummary[]>(buildApiUrl(this.apiBaseUrl, '/api/bookings'));
  }
}
