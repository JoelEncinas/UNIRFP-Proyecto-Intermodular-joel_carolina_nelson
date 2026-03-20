import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { Bike } from '../models/bike.model';
import { BookingSummary, CreateBookingRequest, ReturnBookingRequest } from '../models/map.models';
import { Station } from '../models/station.model';

@Injectable({
  providedIn: 'root',
})
export class MapApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(this.buildUrl('/api/stations'));
  }

  getAvailableBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(this.buildUrl('/api/bikes?status=AVAILABLE'));
  }
  
  getAllBikes(): Observable<Bike[]> {
    return this.http.get<Bike[]>(this.buildUrl('/api/bikes'));
  }

  getMyBookings(): Observable<BookingSummary[]> {
    return this.http.get<BookingSummary[]>(this.buildUrl('/api/bookings'));
  }

  createBooking(payload: CreateBookingRequest): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(this.buildUrl('/api/bookings'), payload);
  }

  activateBooking(bookingId: number): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(this.buildUrl(`/api/bookings/${bookingId}/activate`), {});
  }
  
  returnBooking(bookingId: number, payload: ReturnBookingRequest): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(this.buildUrl(`/api/bookings/${bookingId}/return`), payload);
  }

  private buildUrl(path: string): string {
    if (!this.apiBaseUrl) {
      return path;
    }

    return `${this.apiBaseUrl.replace(/\/+$/, '')}${path}`;
  }
}
