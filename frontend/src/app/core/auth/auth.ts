import { Injectable, inject } from '@angular/core';
import { TokenStorage } from './token-storage';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly tokenStorage = inject(TokenStorage);

  isAuthenticated(): boolean {
    return !!this.tokenStorage.getToken();
  }
}
