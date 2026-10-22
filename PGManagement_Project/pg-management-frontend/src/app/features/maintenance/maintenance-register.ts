
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { MaintenanceRegistrationService } from './maintenance-registration.service';

@Component({
  selector: 'app-maintenance-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './maintenance-register.html',
  styleUrl: './maintenance-register.scss'
})
export class MaintenanceRegisterComponent {
  private readonly registrationService =
    inject(MaintenanceRegistrationService);

  private readonly router = inject(Router);

  name = '';
  mobile = '';
  email = '';
  password = '';
  confirmPassword = '';

  showPassword = false;
  loading = false;
  submitted = false;

  errorMessage = '';
  successMessage = '';

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.submitted = false;

    if (this.name.trim().length < 2) {
      this.errorMessage = 'Please enter your full name.';
      return;
    }

    if (!/^[0-9]{10}$/.test(this.mobile.trim())) {
      this.errorMessage = 'Enter a valid 10-digit mobile number.';
      return;
    }

    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your work email.';
      return;
    }

    if (this.password.length < 12) {
      this.errorMessage =
        'Your password must contain at least 12 characters.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.loading = true;

    this.registrationService.register({
      name: this.name.trim(),
      mobile: this.mobile.trim(),
      email: this.email.trim().toLowerCase(),
      password: this.password
    })
    .pipe(finalize(() => {
      this.loading = false;
    }))
    .subscribe({
      next: () => {
        this.submitted = true;
        this.successMessage =
          'Your registration request was submitted successfully. Please wait for administrator approval before logging in.';

        this.name = '';
        this.mobile = '';
        this.email = '';
        this.password = '';
        this.confirmPassword = '';
      },
      error: (error: HttpErrorResponse) => {
        console.error('Maintenance registration failed:', error);

        this.errorMessage =
          error.error?.detail ||
          error.error?.message ||
          'Unable to submit your registration. Please check your details and try again.';
      }
    });
  }

  goToMaintenanceLogin(): void {
    void this.router.navigate(['/maintenance/login']);
  }
}
