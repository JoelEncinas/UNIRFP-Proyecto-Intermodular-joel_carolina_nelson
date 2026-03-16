import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenStorage {
  private readonly key = 'auth_token';

  getToken(): string | null {
    return sessionStorage.getItem(this.key);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.key, token);
  }

  clearToken(): void {
    sessionStorage.removeItem(this.key);
  }
}
