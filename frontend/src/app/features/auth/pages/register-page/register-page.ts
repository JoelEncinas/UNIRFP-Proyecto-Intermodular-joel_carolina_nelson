import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Auth } from '../../../../core/auth/auth';
import { AuthApi } from '../../data-access/auth-api';
import { RegisterRequest } from '../../models/auth.models';
import { RegisterForm } from '../../ui/register-form/register-form';

@Component({
  selector: 'app-register-page',
  imports: [RegisterForm],
  templateUrl: './register-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage {
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  onRegisterSubmit(payload: RegisterRequest): void {
    if (this.isSubmitting()) {
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    this.authApi
      .register(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.auth.startSession(response.token);
          void this.router.navigateByUrl('/app/map');
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toRegisterErrorMessage(error));
        },
      });
  }

  private toRegisterErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo completar el registro. Intentalo mas tarde.';
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    if (error.status === 409 && apiMessage) {
      const normalized = apiMessage.toLowerCase();
      if (normalized.includes('username')) {
        return 'El usuario ya existe.';
      }
      if (normalized.includes('email')) {
        return 'El email ya existe.';
      }
      return 'No se pudo completar el registro.';
    }

    if (error.status === 400 && apiMessage) {
      return apiMessage;
    }

    return 'No se pudo completar el registro. Intentalo mas tarde.';
  }
}
