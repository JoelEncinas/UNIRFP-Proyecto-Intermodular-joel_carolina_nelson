import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Auth } from '../../../../core/auth/auth';
import { ProfileApi } from '../../data-access/profile-api';
import { ProfileUpdateRequest, ProfileUser } from '../../models/profile.models';

type EditableField = 'username' | 'email' | 'password';

@Component({
  selector: 'app-profile-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit {
  private readonly profileApi = inject(ProfileApi);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly isLoading = signal(true);
  readonly isDeleting = signal(false);
  readonly isDeleteConfirmationVisible = signal(false);
  readonly updateInProgress = signal<EditableField | 'all' | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly profile = signal<ProfileUser | null>(null);

  readonly isBusy = computed(
    () => this.isLoading() || this.isDeleting() || this.updateInProgress() !== null,
  );

  readonly profileForm = this.formBuilder.group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.minLength(6), Validators.maxLength(255)]],
  });

  readonly userInitial = computed(() => {
    const username = this.profile()?.username ?? '';
    if (!username.trim()) {
      return '?';
    }
    return username.trim().charAt(0).toUpperCase();
  });

  ngOnInit(): void {
    this.loadProfile();
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

  private loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
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
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    const apiMessage =
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
        ? error.error.message
        : null;

    if (apiMessage) {
      return apiMessage;
    }

    return fallback;
  }
}
