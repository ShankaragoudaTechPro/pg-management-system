import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import {
  PgService,
  CreatePgRequest
} from '../../../core/services/pg.service';

@Component({
  selector: 'app-pg-create',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './pg-create.html'
})
export class PgCreateComponent {

  private readonly pgService = inject(PgService);
  private readonly router = inject(Router);

  saving = false;
  errorMessage = '';
  successMessage = '';

  pgForm: CreatePgRequest = this.emptyForm();

  private emptyForm(): CreatePgRequest {
    return {
      name: '',
      type: 'GENTS',
      description: ''
    };
  }

  savePg(): void {
    if (this.saving) return;

    this.errorMessage = '';
    this.successMessage = '';

    if (!this.pgForm.name.trim()) {
      this.errorMessage = 'PG name is required.';
      return;
    }

    if (!this.pgForm.type.trim()) {
      this.errorMessage = 'PG type is required.';
      return;
    }


    const request: CreatePgRequest = {
      name: this.pgForm.name.trim(),
      type: this.pgForm.type.trim(),
      description: this.pgForm.description?.trim() || null
    };

    this.saving = true;

    this.pgService
      .createPg(request)
      .pipe(
        timeout({ first: 15000 }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe({
        next: () => {
          this.successMessage = 'PG property created successfully.';

          setTimeout(() => {
            void this.router.navigate(['/dashboard']);
          }, 500);
        },

        error: error => {
          console.error('Create PG error:', error);

          this.errorMessage =
            error?.error?.message ||
            error?.error?.detail ||
            error?.error?.error ||
            (error?.status === 409
              ? 'The server reported a conflict. Check whether this PG name already exists for your account or inspect the backend log for the database constraint.'
              : error?.name === 'TimeoutError'
                ? 'The server took too long to respond. Check the backend logs before retrying.'
                : error?.status === 0
                  ? 'Cannot connect to the backend. Confirm Spring Boot is running on http://localhost:8080.'
                  : 'Unable to create PG property. Check the backend logs for the exact cause.');
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}