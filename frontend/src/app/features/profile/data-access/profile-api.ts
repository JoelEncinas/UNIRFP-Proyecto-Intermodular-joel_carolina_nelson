import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { buildApiUrl } from '../../../core/http/api-url';
import {
  StripeCheckoutSessionCancelRequest,
  ProfileUpdateRequest,
  ProfileUser,
  StripeCheckoutSessionCreateRequest,
  StripeCheckoutSessionResponse,
} from '../models/profile.models';

@Injectable({
  providedIn: 'root',
})
export class ProfileApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getMe(): Observable<ProfileUser> {
    return this.http.get<ProfileUser>(buildApiUrl(this.apiBaseUrl, '/api/users/me'));
  }

  updateMe(payload: ProfileUpdateRequest): Observable<ProfileUser> {
    return this.http.put<ProfileUser>(buildApiUrl(this.apiBaseUrl, '/api/users/me'), payload);
  }

  createStripeCheckoutSession(
    payload: StripeCheckoutSessionCreateRequest,
  ): Observable<StripeCheckoutSessionResponse> {
    return this.http.post<StripeCheckoutSessionResponse>(
      buildApiUrl(this.apiBaseUrl, '/api/payments/checkout-session'),
      payload,
    );
  }

  cancelStripeCheckoutSession(payload: StripeCheckoutSessionCancelRequest): Observable<void> {
    return this.http.post<void>(buildApiUrl(this.apiBaseUrl, '/api/payments/checkout-session/cancel'), payload);
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(buildApiUrl(this.apiBaseUrl, '/api/users/me'));
  }
}
