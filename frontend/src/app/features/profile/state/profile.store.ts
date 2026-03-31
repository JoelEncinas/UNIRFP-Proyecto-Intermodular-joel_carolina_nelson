import { Injectable, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Auth } from '../../../core/auth/auth';
import { getApiErrorMessage } from '../../../core/http/api-error-message';
import { ProfileApi } from '../data-access/profile-api';
import { ProfileUpdateRequest, ProfileUser } from '../models/profile.models';

export type EditableField = 'username' | 'email' | 'password';
export type StripePaymentReturnState = 'success' | 'cancel' | null;

const STRIPE_MIN_TOP_UP_AMOUNT = 0.5;

@Injectable()
export class ProfileStore {
  private readonly profileApi = inject(ProfileApi);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly isLoading = signal(true);
  readonly isDeleting = signal(false);
  readonly isDeleteConfirmationVisible = signal(false);
  readonly updateInProgress = signal<EditableField | 'all' | null>(null);
  readonly topUpInProgress = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly profile = signal<ProfileUser | null>(null);
  readonly topUpAmounts = [5, 10, 20] as const;

  readonly isBusy = computed(
    () =>
      this.isLoading() ||
      this.isDeleting() ||
      this.updateInProgress() !== null ||
      this.topUpInProgress(),
  );

  readonly profileForm = this.formBuilder.group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.minLength(6), Validators.maxLength(255)]],
  });
  readonly topUpForm = this.formBuilder.group({
    topUpAmount: [5, [Validators.required, Validators.min(STRIPE_MIN_TOP_UP_AMOUNT)]],
  });

  readonly userInitial = computed(() => {
    const username = this.profile()?.username ?? '';
    if (!username.trim()) {
      return '?';
    }
    return username.trim().charAt(0).toUpperCase();
  });

  loadInitialData(): void {
    this.loadProfile();
  }

  handleStripeReturn(paymentQuery: StripePaymentReturnState): void {
    if (paymentQuery === 'success') {
      this.successMessage.set('Pago recibido. Actualizando saldo...');
      this.errorMessage.set(null);
      this.loadProfile({
        clearMessages: false,
        successMessage: 'Recarga confirmada. Saldo actualizado.',
      });
      return;
    }

    if (paymentQuery === 'cancel') {
      this.errorMessage.set(null);
      this.successMessage.set('Recarga cancelada. No se ha realizado ningun cargo.');
    }
  }

  selectTopUpAmount(amount: number): void {
    if (this.isBusy()) {
      return;
    }

    this.topUpForm.controls.topUpAmount.setValue(amount);
    this.errorMessage.set(null);
  }

  startStripeTopUp(): void {
    if (this.isBusy()) {
      return;
    }

    const userId = this.auth.getAuthenticatedUserId();
    if (userId === null) {
      this.errorMessage.set('Tu sesion ha caducado. Inicia sesion de nuevo.');
      return;
    }

    const amountControl = this.topUpForm.controls.topUpAmount;
    if (amountControl.invalid) {
      amountControl.markAsTouched();
      this.errorMessage.set('El importe minimo es 0.50 EUR.');
      return;
    }

    const amount = this.normalizeAmount(amountControl.value);
    if (amount < STRIPE_MIN_TOP_UP_AMOUNT) {
      this.errorMessage.set('El importe minimo es 0.50 EUR.');
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.topUpInProgress.set(true);

    this.profileApi
      .createStripeCheckoutSession({ userId, amount })
      .pipe(finalize(() => this.topUpInProgress.set(false)))
      .subscribe({
        next: (response) => {
          window.location.assign(response.checkoutUrl);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toErrorMessage(error, 'No se pudo iniciar el pago con Stripe.'));
        },
      });
  }

  updateField(field: EditableField): void {
    if (this.isBusy()) {
      return;
    }

    const payload = this.buildSingleFieldPayload(field);
    if (!payload) {
      return;
    }

    this.applyUpdate(payload, field, `Campo ${field} actualizado.`);
  }

  updateAll(): void {
    if (this.isBusy()) {
      return;
    }

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const value = this.profileForm.getRawValue();
    const password = value.password.trim();
    const payload: ProfileUpdateRequest = {
      username: value.username.trim(),
      email: value.email.trim(),
    };

    if (password) {
      payload.password = password;
    }

    this.applyUpdate(payload, 'all', 'Perfil actualizado.');
  }

  showDeleteConfirmation(): void {
    if (this.isBusy()) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isDeleteConfirmationVisible.set(true);
  }

  cancelDeleteConfirmation(): void {
    if (this.isDeleting()) {
      return;
    }

    this.isDeleteConfirmationVisible.set(false);
  }

  confirmDeleteAccount(): void {
    if (this.isBusy()) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isDeleting.set(true);

    this.profileApi
      .deleteMe()
      .pipe(
        finalize(() => {
          this.isDeleting.set(false);
        }),
      )
      .subscribe({
        next: () => {
          this.isDeleteConfirmationVisible.set(false);
          this.auth.logout();
          void this.router.navigateByUrl('/auth/login');
        },
        error: (error: unknown) => {
          this.isDeleteConfirmationVisible.set(true);
          this.errorMessage.set(this.toErrorMessage(error, 'No se pudo eliminar la cuenta.'));
        },
      });
  }

  private loadProfile(options?: { clearMessages?: boolean; successMessage?: string }): void {
    const clearMessages = options?.clearMessages ?? true;

    this.isLoading.set(true);
    this.errorMessage.set(null);
    if (clearMessages) {
      this.successMessage.set(null);
    }
    this.isDeleteConfirmationVisible.set(false);

    this.profileApi
      .getMe()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.profileForm.setValue({
            username: profile.username,
            email: profile.email,
            password: '',
          });
          if (options?.successMessage) {
            this.successMessage.set(options.successMessage);
          }
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toErrorMessage(error, 'No se pudo cargar el perfil.'));
        },
      });
  }

  private buildSingleFieldPayload(field: EditableField): ProfileUpdateRequest | null {
    const currentProfile = this.profile();
    if (!currentProfile) {
      return null;
    }

    if (field === 'password') {
      const password = this.profileForm.controls.password.value.trim();
      if (!password) {
        this.errorMessage.set('Introduce una contrasena para actualizarla.');
        return null;
      }
      if (this.profileForm.controls.password.invalid) {
        this.profileForm.controls.password.markAsTouched();
        this.errorMessage.set('La contrasena debe tener al menos 6 caracteres.');
        return null;
      }
      return { password };
    }

    const control = this.profileForm.controls[field];
    if (control.invalid) {
      control.markAsTouched();
      return null;
    }

    const newValue = control.value.trim();
    const oldValue = currentProfile[field];
    if (newValue === oldValue) {
      this.successMessage.set('No hay cambios para guardar.');
      this.errorMessage.set(null);
      return null;
    }

    return { [field]: newValue };
  }

  private applyUpdate(
    payload: ProfileUpdateRequest,
    mode: EditableField | 'all',
    successMessage: string,
  ): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isDeleteConfirmationVisible.set(false);
    this.updateInProgress.set(mode);

    this.profileApi
      .updateMe(payload)
      .pipe(finalize(() => this.updateInProgress.set(null)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.profileForm.patchValue({
            username: profile.username,
            email: profile.email,
            password: '',
          });
          this.successMessage.set(successMessage);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.toErrorMessage(error, 'No se pudo actualizar el perfil.'));
        },
      });
  }

  private toErrorMessage(error: unknown, fallback: string): string {
    return getApiErrorMessage(error) ?? fallback;
  }

  private normalizeAmount(amount: number): number {
    return Math.round(amount * 100) / 100;
  }
}
