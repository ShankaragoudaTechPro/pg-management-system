import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './forgot-password.html'
})
export class ForgotPasswordComponent {

  private readonly router = inject(Router);

  mobile = '';
  loading = false;
  errorMessage = '';

  sendOtp(): void {
    this.errorMessage = '';

    if (!this.mobile.trim()) {
      this.errorMessage = 'Please enter your registered mobile number.';
      return;
    }

    /*
     * UI is ready.
     * Connect this action to your existing Spring Boot
     * forgot-password SMS OTP endpoint once the exact
     * request/response DTO is confirmed.
     */

    this.loading = true;

    setTimeout(() => {
      this.loading = false;
    }, 700);
  }

  backToLogin(): void {
    this.router.navigate(['/login']);
  }
}