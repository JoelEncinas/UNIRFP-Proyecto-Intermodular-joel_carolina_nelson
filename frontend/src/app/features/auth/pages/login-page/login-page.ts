import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Auth } from '../../../../core/auth/auth';
import { AuthApi } from '../../data-access/auth-api';
import { LoginRequest } from '../../models/auth.models';
import { LoginForm } from '../../ui/login-form/login-form';

@Component({
  selector: 'app-login-page',
  imports: [LoginForm, RouterLink],
  templateUrl: './login-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  onLoginSubmit(payload: LoginRequest): void {
    if (this.isSubmitting()) {
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    this.authApi
      .login(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.auth.startSession(response.token);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          void this.router.navigateByUrl(this.sanitizeReturnUrl(returnUrl));
        },
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.errorMessage.set('Usuario o contrasena invalidos.');
            return;
          }
          this.errorMessage.set('No se pudo acceder. Intentalo mas tarde.');
        },
      });
  }

  private sanitizeReturnUrl(returnUrl: string | null): string {
    if (returnUrl && returnUrl.startsWith('/app/')) {
      return returnUrl;
    }
    return '/app/map';
  }
}

