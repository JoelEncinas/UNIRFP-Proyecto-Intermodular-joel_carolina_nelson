import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { buildApiUrl } from '../../../core/http/api-url';
import { Bike } from '../../../shared/domain/bike.model';
import { BookingSummary } from '../../../shared/domain/booking.model';
import { Station } from '../../../shared/domain/station.model';
import {
  BookingCreatePaymentResponse,
  BookingStripeSessionRequest,
  CreateBookingRequest,
  MapCurrentUser,
  MapPaymentConfig,
  ReturnBookingRequest,
} from '../models/map.models';

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

  getMe(): Observable<MapCurrentUser> {
    return this.http.get<MapCurrentUser>(buildApiUrl(this.apiBaseUrl, '/api/users/me'));
  }

  getPaymentConfig(): Observable<MapPaymentConfig> {
    return this.http.get<MapPaymentConfig>(buildApiUrl(this.apiBaseUrl, '/api/payments/config'));
  }

  createBooking(payload: CreateBookingRequest): Observable<BookingCreatePaymentResponse> {
    return this.http.post<BookingCreatePaymentResponse>(buildApiUrl(this.apiBaseUrl, '/api/bookings'), payload);
  }

  finalizeStripeUnlock(
    bookingId: number,
    payload: BookingStripeSessionRequest,
  ): Observable<BookingCreatePaymentResponse> {
    return this.http.post<BookingCreatePaymentResponse>(
      buildApiUrl(this.apiBaseUrl, `/api/bookings/${bookingId}/stripe/finalize`),
      payload,
    );
  }

  cancelStripeUnlock(
    bookingId: number,
    payload: BookingStripeSessionRequest,
  ): Observable<BookingCreatePaymentResponse> {
    return this.http.post<BookingCreatePaymentResponse>(
      buildApiUrl(this.apiBaseUrl, `/api/bookings/${bookingId}/stripe/cancel`),
      payload,
    );
  }
  
  returnBooking(bookingId: number, payload: ReturnBookingRequest): Observable<BookingSummary> {
    return this.http.post<BookingSummary>(
      buildApiUrl(this.apiBaseUrl, `/api/bookings/${bookingId}/return`),
      payload,
    );
  }
}
