
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
  styleUrl: './login.scss'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentYear = new Date().getFullYear();

  email = '';
  password = '';
  showPassword = false;
  loading = false;
  errorMessage = '';

  login(): void {
    this.errorMessage = '';

    if (this.loading) return;

    const email = this.email.trim().toLowerCase();

    if (!email || !this.password) {
      this.errorMessage = 'Enter your email and password.';
      return;
    }

    this.loading = true;

    this.authService.login({
      email,
      password: this.password
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: response => {
        if (!response?.token) {
          this.errorMessage =
            'Login response did not contain a token. Check the backend response.';
          return;
        }

        // AuthService stores the token.
        void this.router.navigate(['/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Owner login failed:', error);

        this.errorMessage =
          error.error?.message ??
          error.error?.detail ??
          (error.status === 0
            ? 'Cannot connect to the backend. Check that Spring Boot is running on port 8080.'
            : error.status === 401
              ? 'Invalid email or password.'
              : error.status === 403
                ? 'Access denied. Check the backend security configuration.'
                : `Login failed with HTTP ${error.status}.`);
      }
    });
  }
}
