
import { CommonModule } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';

import { RentService } from './rent.service';
import {
  PaymentMode,
  RentRecord,
  RentStatus,
  RecordPaymentRequest
} from './rent.model';

@Component({
  selector: 'app-rent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rent.html',
  styleUrl: './rent.scss'
})
export class RentComponent implements OnInit, OnDestroy {
  private readonly rentService = inject(RentService);

  rents: RentRecord[] = [];

  loading = false;
  savingPayment = false;

  errorMessage = '';
  successMessage = '';

  searchText = '';
  selectedStatus = 'ALL';
  selectedMonth = 'ALL';

  paymentModalOpen = false;
  selectedRent: RentRecord | null = null;

  paymentForm: RecordPaymentRequest =
    this.emptyPaymentForm();

  readonly paymentModes: PaymentMode[] = [
    'CASH',
    'UPI',
    'BANK_TRANSFER',
    'OTHER'
  ];

  private messageTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.loadRents();
  }

  ngOnDestroy(): void {
    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
    }
  }

  loadRents(): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.rentService
      .getAll({
        page: 0,
        size: 100,
        search: this.searchText.trim() || undefined,
        status: this.selectedStatus
      })
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => (this.loading = false))
      )
      .subscribe({
        next: response => {
          if (Array.isArray(response)) {
            this.rents = response;
          } else if (
            response &&
            Array.isArray(response.content)
          ) {
            this.rents = response.content;
          } else {
            this.rents = [];
          }
        },
        error: error => {
          console.error('Failed to load rents:', error);
          this.errorMessage = error?.name === 'TimeoutError'
            ? 'Rent request timed out after 10 seconds. Check the backend and database.'
            : error?.status === 0
              ? 'Cannot connect to the backend. Check that Spring Boot is running.'
              : error?.status === 401
                ? 'Your session has expired. Please sign in again.'
                : error?.status === 403
                  ? 'You do not have permission to view these rent records.'
                  : error?.error?.message || 'Unable to load rent records. Please try again.';
        }
      });
  }

  get filteredRents(): RentRecord[] {
    const search = this.searchText.trim().toLowerCase();

    return this.rents.filter(rent => {
      const matchesSearch =
        !search ||
        [
          rent.tenantName,
          rent.pgName,
          rent.roomNumber,
          rent.bedNumber,
          rent.rentMonth
        ].some(value =>
          String(value ?? '').toLowerCase().includes(search)
        );

      const matchesStatus =
        this.selectedStatus === 'ALL' ||
        this.effectiveStatus(rent) === this.selectedStatus;

      const matchesMonth =
        this.selectedMonth === 'ALL' ||
        rent.rentMonth === this.selectedMonth;

      return matchesSearch && matchesStatus && matchesMonth;
    });
  }

  get months(): string[] {
    return [
      ...new Set(
        this.rents
          .map(rent => rent.rentMonth)
          .filter(Boolean)
      )
    ].sort((a, b) => b.localeCompare(a));
  }

  get totalExpected(): number {
    return this.filteredRents.reduce(
      (total, rent) => total + this.amountOf(rent),
      0
    );
  }

  get totalCollected(): number {
    return this.filteredRents.reduce(
      (total, rent) => total + this.paidOf(rent),
      0
    );
  }

  get totalOutstanding(): number {
    return this.filteredRents.reduce(
      (total, rent) => total + this.balanceOf(rent),
      0
    );
  }

  get totalOverdue(): number {
    return this.filteredRents.reduce(
      (total, rent) =>
        this.effectiveStatus(rent) === 'OVERDUE'
          ? total + this.balanceOf(rent)
          : total,
      0
    );
  }

  get pendingCount(): number {
    return this.filteredRents.filter(rent => {
      const status = this.effectiveStatus(rent);
      return status === 'PENDING' || status === 'OVERDUE';
    }).length;
  }

  amountOf(rent: RentRecord): number {
    return Math.max(0, Number(rent.amount) || 0);
  }

  paidOf(rent: RentRecord): number {
    return Math.max(0, Number(rent.paidAmount) || 0);
  }

  balanceOf(rent: RentRecord): number {
    return Math.max(
      0,
      this.amountOf(rent) - this.paidOf(rent)
    );
  }

  effectiveStatus(rent: RentRecord): RentStatus {
    if (this.balanceOf(rent) <= 0) {
      return 'PAID';
    }

    if (rent.status === 'OVERDUE' || this.isPastDue(rent.dueDate)) {
      return 'OVERDUE';
    }

    return 'PENDING';
  }

  private isPastDue(dueDate?: string): boolean {
    if (!dueDate) {
      return false;
    }

    const due = new Date(`${dueDate.slice(0, 10)}T00:00:00`);
    const todayText = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Kolkata',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(new Date());

    const today = new Date(`${todayText}T00:00:00`);

    return !Number.isNaN(due.getTime()) && due < today;
  }

  statusLabel(rent: RentRecord): string {
    const status = this.effectiveStatus(rent);

    const labels: Record<RentStatus, string> = {
      PENDING: 'Pending',
      PAID: 'Paid',
      OVERDUE: 'Overdue'
    };

    return labels[status];
  }

  openPaymentModal(rent: RentRecord): void {
    if (this.balanceOf(rent) <= 0) {
      return;
    }

    this.selectedRent = rent;
    this.paymentForm = {
      paidDate: this.todayInIndia(),
      paymentMode: 'UPI',
      paymentNote: ''
    };

    this.errorMessage = '';
    this.paymentModalOpen = true;
  }

  closePaymentModal(): void {
    if (this.savingPayment) {
      return;
    }

    this.paymentModalOpen = false;
    this.selectedRent = null;
    this.errorMessage = '';
  }

  submitPayment(): void {
    const rent = this.selectedRent;

    if (!rent) {
      this.errorMessage = 'Please select a rent record.';
      return;
    }

    if (!this.paymentForm.paidDate) {
      this.errorMessage = 'Please select a payment date.';
      return;
    }

    if (!this.paymentForm.paymentMode) {
      this.errorMessage = 'Please select a payment mode.';
      return;
    }

    const payload: RecordPaymentRequest = {
      paidDate: this.paymentForm.paidDate,
      paymentMode: this.paymentForm.paymentMode,
      paymentNote: this.paymentForm.paymentNote?.trim() || undefined
    };

    this.savingPayment = true;
    this.errorMessage = '';

    this.rentService
      .recordPayment(rent.id, payload)
      .pipe(finalize(() => (this.savingPayment = false)))
      .subscribe({
        next: () => {
          this.paymentModalOpen = false;
          this.selectedRent = null;
          this.showSuccess('Payment recorded successfully.');
          this.loadRents();
        },
        error: error => {
          console.error('Failed to record payment:', error);
          this.errorMessage =
            error?.error?.message ||
            'Unable to record payment. Please check the backend API.';
        }
      });
  }

  clearFilters(): void {
    this.searchText = '';
    this.selectedStatus = 'ALL';
    this.selectedMonth = 'ALL';
    this.loadRents();
  }

  trackById(
    _index: number,
    rent: RentRecord
  ): number {
    return rent.id;
  }

  private todayInIndia(): string {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Kolkata',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(new Date());
  }

  private emptyPaymentForm(): RecordPaymentRequest {
    return {
      paidDate: this.todayInIndia(),
      paymentMode: 'UPI',
      paymentNote: ''
    };
  }

  private showSuccess(message: string): void {
    this.successMessage = message;

    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
    }

    this.messageTimer = setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }
}
