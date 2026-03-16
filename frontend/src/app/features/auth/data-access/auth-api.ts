import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  // v1 auth contract: login uses username + password and returns only { token }.
  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.buildUrl('/auth/login'), payload);
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.buildUrl('/auth/register'), payload);
  }

  private buildUrl(path: '/auth/login' | '/auth/register'): string {
    if (!this.apiBaseUrl) {
      return path;
    }

    return `${this.apiBaseUrl.replace(/\/+$/, '')}${path}`;
  }
}
