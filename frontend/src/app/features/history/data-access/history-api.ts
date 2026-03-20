import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { BookingSummary } from '../../map/models/map.models';

@Injectable({
  providedIn: 'root',
})
export class HistoryApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getMyBookings(): Observable<BookingSummary[]> {
    return this.http.get<BookingSummary[]>(this.buildUrl('/api/bookings'));
  }

  private buildUrl(path: '/api/bookings'): string {
    if (!this.apiBaseUrl) {
      return path;
    }

    return `${this.apiBaseUrl.replace(/\/+$/, '')}${path}`;
  }
}
