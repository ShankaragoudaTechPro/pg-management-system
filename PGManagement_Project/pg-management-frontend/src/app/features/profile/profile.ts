import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, Subscription, timeout } from 'rxjs';

import { OwnerProfile, UpdatePasswordRequest } from './profile.model';
import { ProfileService } from './profile.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly profileService = inject(ProfileService);
  private profileRequest?: Subscription;
  private saveRequest?: Subscription;
  private passwordRequest?: Subscription;

  profile: OwnerProfile | null = null;
  editMode = false;
  loading = false;
  saving = false;
  changingPassword = false;
  showPassword = false;
  showConfirmPassword = false;
  successMessage = '';
  errorMessage = '';

  passwordForm: UpdatePasswordRequest = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  ngOnInit(): void { this.loadProfile(); }

  ngOnDestroy(): void {
    this.profileRequest?.unsubscribe();
    this.saveRequest?.unsubscribe();
    this.passwordRequest?.unsubscribe();
  }

  get initials(): string {
    const name = this.profile?.name?.trim() ?? '';
    return name ? name.split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase() : 'PG';
  }

  loadProfile(): void {
    this.profileRequest?.unsubscribe();
    this.loading = true;
    this.clearMessages();
    this.profileRequest = this.profileService.getProfile()
      .pipe(timeout({ first: 10000 }), finalize(() => this.loading = false))
      .subscribe({
        next: value => {
          this.profile = value;
          this.editMode = false;
        },
        error: error => {
          console.error('Load profile failed:', error);
          this.errorMessage = this.errorText(error, 'Unable to load your profile.');
        }
      });
  }

  toggleEdit(): void {
    if (!this.profile || this.loading || this.saving) return;
    this.editMode = !this.editMode;
    this.clearMessages();
  }

  cancelEdit(): void {
    this.editMode = false;
    this.loadProfile();
  }

  saveProfile(): void {
    if (!this.profile || this.saving) return;
    const name = this.profile.name.trim();
    const mobile = this.profile.mobile.trim();

    if (name.length < 3 || name.length > 100) {
      this.errorMessage = 'Name must contain between 3 and 100 characters.';
      return;
    }
    if (!/^[6-9][0-9]{9}$/.test(mobile)) {
      this.errorMessage = 'Enter a valid 10-digit Indian mobile number.';
      return;
    }

    this.saving = true;
    this.clearMessages();
    this.saveRequest?.unsubscribe();
    this.saveRequest = this.profileService.updateProfile({
      name,
      mobile,
      email: this.profile.email
    }).pipe(timeout({ first: 10000 }), finalize(() => this.saving = false)).subscribe({
      next: updated => {
        this.profile = updated;
        this.editMode = false;
        this.successMessage = 'Profile saved to your account.';
      },
      error: error => {
        console.error('Save profile failed:', error);
        this.errorMessage = this.errorText(error, 'Unable to save profile.');
      }
    });
  }

  updatePassword(): void {
    if (this.changingPassword) return;
    const { currentPassword, newPassword, confirmPassword } = this.passwordForm;
    this.clearMessages();

    if (!currentPassword || !newPassword || !confirmPassword) {
      this.errorMessage = 'Complete all password fields.';
      return;
    }
    if (newPassword.length < 8) {
      this.errorMessage = 'New password must contain at least 8 characters.';
      return;
    }
    if (newPassword === currentPassword) {
      this.errorMessage = 'New password must differ from the current password.';
      return;
    }
    if (newPassword !== confirmPassword) {
      this.errorMessage = 'New password and confirmation do not match.';
      return;
    }

    this.changingPassword = true;
    this.passwordRequest?.unsubscribe();
    this.passwordRequest = this.profileService.changePassword({
      currentPassword,
      newPassword,
      confirmPassword
    }).pipe(timeout({ first: 10000 }), finalize(() => this.changingPassword = false)).subscribe({
      next: message => {
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.successMessage = message || 'Password changed successfully.';
      },
      error: error => {
        console.error('Change password failed:', error);
        this.errorMessage = this.errorText(error, 'Unable to change password.');
      }
    });
  }

  getPasswordMismatch(): boolean {
    return !!this.passwordForm.newPassword &&
      !!this.passwordForm.confirmPassword &&
      this.passwordForm.newPassword !== this.passwordForm.confirmPassword;
  }

  private errorText(error: HttpErrorResponse, fallback: string): string {
    const body = error.error;
    if (body && typeof body === 'object') {
      const message = body.message ?? body.detail ?? body.error;
      if (typeof message === 'string' && message.trim()) return message;
    }
    if (typeof body === 'string' && body.trim()) return body;
    if (error.status === 0) return 'Cannot connect to the backend. Check that Spring Boot is running.';
    if (error.status === 401) return 'Your session expired. Please sign in again.';
    if (error.status === 403) return 'You do not have permission to perform this action.';
    return fallback;
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }
}
