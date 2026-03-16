import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import { AuthApi } from './auth-api';

describe('AuthApi', () => {
  let authApi: AuthApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://localhost:8080' },
      ],
    });

    authApi = TestBed.inject(AuthApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('login should POST to /auth/login with username credentials', () => {
    const payload: LoginRequest = {
      username: 'rider_1',
      password: 'secret123',
    };

    let response: AuthResponse | undefined;
    authApi.login(payload).subscribe((result) => {
      response = result;
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({ token: 'jwt-token-1' });
    expect(response).toEqual({ token: 'jwt-token-1' });
  });

  it('register should POST to /auth/register and return a typed token response', () => {
    const payload: RegisterRequest = {
      username: 'rider_2',
      email: 'rider2@example.com',
      password: 'secret123',
    };

    let response: AuthResponse | undefined;
    authApi.register(payload).subscribe((result) => {
      response = result;
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({ token: 'jwt-token-2' });
    expect(response).toEqual({ token: 'jwt-token-2' });
  });
});
