
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentYear: number = new Date().getFullYear();

  email = '';
  password = '';
  showPassword = false;
  loading = false;
  errorMessage = '';

  login(): void {
    this.errorMessage = '';

    if (this.loading) {
      return;
    }

    const normalizedEmail = this.email.trim().toLowerCase();

    if (!normalizedEmail || !this.password) {
      this.errorMessage = 'Please enter your email and password.';
      return;
    }

    this.loading = true;

    this.authService
      .login({
        email: normalizedEmail,
        password: this.password
      })
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (response) => {
          if (!response?.token) {
            this.errorMessage =
              'Login response did not contain a token. Please check your backend response.';
            return;
          }

          void this.router.navigate(['/dashboard']);
        },

        error: (error: HttpErrorResponse) => {
          console.error('Login failed:', error);

          this.errorMessage =
            error.error?.message ??
            error.error?.detail ??
            (error.status === 0
              ? 'Unable to connect to the server. Check whether the backend is running.'
              : error.status === 400
                ? 'Please check the information you entered.'
                : error.status === 401
                  ? 'Invalid email or password.'
                  : error.status === 403
                    ? 'Access denied. Please check your account permissions.'
                    : `Login failed. Server returned HTTP ${error.status}.`);
        }
      });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}
