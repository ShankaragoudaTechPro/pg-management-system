
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { MaintenanceAuthService } from '../../core/services/maintenance-auth.service';

interface MaintenanceLoginResponse {
  token: string;
  tokenType: string;
  staffId: number;
  name: string;
  email: string;
  role: string;
}

@Component({
  selector: 'app-maintenance-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './maintenance-login.html',
  styleUrl: './maintenance-login.scss'
})
export class MaintenanceLoginComponent {
  private readonly authService = inject(MaintenanceAuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  showPassword = false;
  loading = false;
  errorMessage = '';

  login(): void {
    this.errorMessage = '';

    if (!this.email.trim() || !this.password) {
      this.errorMessage = 'Enter your email and password.';
      return;
    }

    this.loading = true;

    this.authService.login({
      email: this.email.trim().toLowerCase(),
      password: this.password
    })
    .pipe(finalize(() => {
      this.loading = false;
    }))
    .subscribe({
      next: (response: MaintenanceLoginResponse) => {
        if (response.role !== 'SUPER_MAINTENANCE') {
          this.authService.logout();
          this.errorMessage =
            'This account is not authorized for maintenance access.';
          return;
        }

        void this.router.navigate(['/maintenance/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ||
          error.error?.detail ||
          'Login failed. Check your credentials and try again.';
      }
    });
  }
}
