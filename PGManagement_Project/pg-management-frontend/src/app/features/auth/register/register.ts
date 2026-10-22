
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;

  registerForm = this.fb.nonNullable.group({
    name: ['', [
      Validators.required,
      Validators.minLength(2)
    ]],
    mobile: ['', [
      Validators.required,
      Validators.pattern(/^[6-9]\d{9}$/)
    ]],
    email: ['', [
      Validators.required,
      Validators.email
    ]],
    password: ['', [
      Validators.required,
      Validators.minLength(6)
    ]]
  });

  get name() {
    return this.registerForm.controls.name;
  }

  get mobile() {
    return this.registerForm.controls.mobile;
  }

  get email() {
    return this.registerForm.controls.email;
  }

  get password() {
    return this.registerForm.controls.password;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  goToLogin(): void {
    void this.router.navigate(['/login']);
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.loading) {
      return;
    }

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.errorMessage =
        'Please correct the highlighted fields and try again.';
      return;
    }

    this.loading = true;

    const request = this.registerForm.getRawValue();

    this.authService.register({
      name: request.name.trim(),
      mobile: request.mobile.trim(),
      email: request.email.trim(),
      password: request.password
    }).pipe(
      // Prevent the page from remaining in loading state indefinitely.
      timeout({ first: 15000 }),

      // Always clear the loading state on success, failure or timeout.
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (response) => {
        this.successMessage =
          response?.message ||
          'Registration successful. Redirecting to login...';

        this.registerForm.reset();

        setTimeout(() => {
          void this.router.navigate(['/login']);
        }, 1200);
      },

      error: (error: unknown) => {
        this.errorMessage = this.getErrorMessage(error);
      }
    });
  }

  private getErrorMessage(error: unknown): string {
    if (
      error instanceof Error &&
      error.name === 'TimeoutError'
    ) {
      return 'Registration timed out. Check that Spring Boot is running and inspect the backend logs.';
    }

    if (!(error instanceof HttpErrorResponse)) {
      return error instanceof Error
        ? error.message
        : 'Registration failed. Please try again.';
    }

    if (error.status === 0) {
      return 'Cannot connect to the backend. Check that Spring Boot is running on port 8080.';
    }

    const body = error.error as {
      message?: string;
      error?: string;
      detail?: string;
      errors?: unknown;
    } | string | null;

    if (typeof body === 'string' && body.trim()) {
      return body;
    }

    if (body && typeof body === 'object') {
      if (body.message) {
        return body.message;
      }

      if (body.detail) {
        return body.detail;
      }

      if (body.error) {
        return body.error;
      }

      if (Array.isArray(body.errors)) {
        return body.errors
          .map(item => {
            if (typeof item === 'string') {
              return item;
            }

            if (item && typeof item === 'object') {
              const entry = item as Record<string, unknown>;
              return entry['defaultMessage'] ??
                entry['message'] ??
                '';
            }

            return '';
          })
          .filter(Boolean)
          .join(', ');
      }
    }

    if (error.status === 400) {
      return 'The backend rejected the registration details. Check the response in Network for the exact reason.';
    }

    if (error.status === 409) {
      return 'This email address or mobile number may already be registered.';
    }

    return `Registration failed (HTTP ${error.status}). Please try again.`;
  }
}
