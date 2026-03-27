import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { buildApiUrl } from '../../../core/http/api-url';
import { Bike } from '../../../shared/domain/bike.model';
import { BookingSummary } from '../../../shared/domain/booking.model';
import { Station } from '../../../shared/domain/station.model';
import { CreateBookingRequest, ReturnBookingRequest } from '../models/map.models';

@Injectable({
  providedIn: 'root',
})
export class MapApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(buildApiUrl(this.apiBaseUrl, '/api/stations'));
  }

  getAvailableBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(buildApiUrl(this.apiBaseUrl, '/api/bikes?status=AVAILABLE'));
  }
  
  getAllBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(buildApiUrl(this.apiBaseUrl, '/api/bikes'));
  }

  getMyBookings(): Observable<BookingSummary[]> {
    return this.http.get<BookingSummary[]>(buildApiUrl(this.apiBaseUrl, '/api/bookings'));
  }

  createBooking(payload: CreateBookingRequest): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(buildApiUrl(this.apiBaseUrl, '/api/bookings'), payload);
  }

  activateBooking(bookingId: number): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(
      buildApiUrl(this.apiBaseUrl, `/api/bookings/${bookingId}/activate`),
      {},
    );
  }
  
  returnBooking(bookingId: number, payload: ReturnBookingRequest): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(
      buildApiUrl(this.apiBaseUrl, `/api/bookings/${bookingId}/return`),
      payload,
    );
  }
}
