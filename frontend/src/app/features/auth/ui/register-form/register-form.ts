import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { RegisterRequest } from '../../models/auth.models';

@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule],
  templateUrl: './register-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterForm {
  private readonly formBuilder = inject(NonNullableFormBuilder);

  @Input() loading = false;
  @Output() submitRegister = new EventEmitter<RegisterRequest>();

  readonly registerForm = this.formBuilder.group({
    username: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  onSubmit(): void {
    if (this.loading || this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.submitRegister.emit(this.registerForm.getRawValue());
  }
}
