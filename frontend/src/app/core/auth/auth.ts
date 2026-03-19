import { Injectable, inject } from '@angular/core';
import { TokenStorage } from './token-storage';

interface JwtPayload {
  exp?: number;
  uid?: number;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly tokenStorage = inject(TokenStorage);

  // Call this after successful /auth/login or /auth/register responses.
  startSession(token: string): void {
    this.tokenStorage.setToken(token);
  }

  logout(): void {
    this.tokenStorage.clearToken();
  }

  getValidToken(): string | null {
    const token = this.tokenStorage.getToken();
    if (!token) {
      return null;
    }

    const payload = this.parseJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number' || typeof payload.uid !== 'number') {
      this.logout();
      return null;
    }

    const nowInSeconds = Math.floor(Date.now() / 1000);
    if (payload.exp <= nowInSeconds) {
      this.logout();
      return null;
    }

    return token;
  }

  isAuthenticated(): boolean {
    return this.getValidToken() !== null;
  }

  private parseJwtPayload(token: string): JwtPayload | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    try {
      const base64Url = parts[1];
      const base64 = this.padBase64(base64Url.replace(/-/g, '+').replace(/_/g, '/'));
      const jsonPayload = atob(base64);
      return JSON.parse(jsonPayload) as JwtPayload;
    } catch {
      return null;
    }
  }

  private padBase64(value: string): string {
    const remainder = value.length % 4;
    if (remainder === 0) {
      return value;
    }
    return value + '='.repeat(4 - remainder);
  }
}
