
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import {
  finalize,
  timeout,
  TimeoutError
} from 'rxjs';

import {
  CreatePgRequest,
  PgService
} from '../../../core/services/pg.service';
import { PgProperty } from '../../../core/models/pg.model';
import { DashboardResponse } from '../../../core/models/dashboard.model';
import { DashboardService } from '../../../core/services/dashboard.service';

type ViewMode = 'grid' | 'table';

type SortOption =
  | 'occupancy-desc'
  | 'occupancy-asc'
  | 'rent-desc'
  | 'vacancy-first';

@Component({
  selector: 'app-pg-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './pg-list.html',
  styleUrl: './pg-list.scss'
})
export class PgListComponent implements OnInit {
  private readonly pgService = inject(PgService);
  private readonly dashboardService = inject(DashboardService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // PROPERTY DATA
  pgs: PgProperty[] = [];
  dashboardByPg: Record<number, DashboardResponse> = {};

  // LOADING STATES
  // Start as false so the first API request is not skipped.
  loading = false;
  metricsLoading = false;
  saving = false;

  // MESSAGES
  errorMessage = '';
  toastMessage = '';
  saveError = '';

  // FILTERS AND VIEW
  searchTerm = '';
  selectedType = 'ALL';
  sortOption: SortOption = 'occupancy-desc';
  viewMode: ViewMode = 'grid';

  // ADD PROPERTY MODAL
  isAddModalOpen = false;

  private toastTimer?: ReturnType<typeof setTimeout>;

  readonly addPgForm = this.fb.group({
    name: [
      '',
      [
        Validators.required,
        Validators.maxLength(150)
      ]
    ],
    type: [
      'COLIVING',
      [
        Validators.required,
        Validators.maxLength(60)
      ]
    ],
    description: [
      '',
      Validators.maxLength(1000)
    ]
  });

  ngOnInit(): void {
    this.loadProperties();
  }

  // LOAD PG PROPERTIES

  loadProperties(): void {
    // Prevent duplicate property-list requests.
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.pgService.getMyPgs()
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (response) => {
          this.pgs = Array.isArray(response) ? response : [];
          this.errorMessage = '';

          // Load dashboard data after the property list succeeds.
          this.loadDashboardMetrics();
        },

        error: (error: unknown) => {
          console.error(
            '[PG LIST] Properties request failed:',
            error
          );

          this.metricsLoading = false;
          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to load PG properties. Please try again.'
          );
        }
      });
  }

  // LOAD DASHBOARD METRICS

  loadDashboardMetrics(): void {
    if (this.pgs.length === 0) {
      this.dashboardByPg = {};
      this.metricsLoading = false;
      return;
    }

    if (this.metricsLoading) {
      return;
    }

    this.metricsLoading = true;

    this.dashboardService.getDashboard()
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.metricsLoading = false;
        })
      )
      .subscribe({
        next: (response) => {
          const metrics: Record<number, DashboardResponse> = {};

          for (const item of response ?? []) {
            metrics[item.pgId] = item;
          }

          this.dashboardByPg = metrics;
        },

        error: (error: unknown) => {
          console.warn(
            '[PG LIST] Dashboard metrics unavailable:',
            error
          );

          // Keep the property list visible if metrics fail.
          // Previously loaded dashboard metrics are not cleared here.
        }
      });
  }

  // FILTER OPTIONS

  get typeOptions(): string[] {
    return Array.from(
      new Set(
        this.pgs
          .map(pg => (pg.type ?? '').trim())
          .filter(Boolean)
      )
    ).sort((a, b) => a.localeCompare(b));
  }

  // FILTERED AND SORTED PROPERTIES

  get filteredPgs(): PgProperty[] {
    const keyword = this.searchTerm.trim().toLowerCase();

    const filtered = this.pgs.filter(pg => {
      const searchableText = [
        pg.name,
        pg.type,
        pg.status,
        pg.description ?? ''
      ]
        .join(' ')
        .toLowerCase();

      const matchesSearch =
        !keyword || searchableText.includes(keyword);

      const matchesType =
        this.selectedType === 'ALL' ||
        (pg.type ?? '').toLowerCase() ===
          this.selectedType.toLowerCase();

      return matchesSearch && matchesType;
    });

    return [...filtered].sort((a, b) => {
      const first = this.getMetrics(a.id);
      const second = this.getMetrics(b.id);

      switch (this.sortOption) {
        case 'occupancy-asc':
          return first.occupancy - second.occupancy;

        case 'rent-desc':
          return second.rentCollected - first.rentCollected;

        case 'vacancy-first':
          return second.availableBeds - first.availableBeds;

        case 'occupancy-desc':
        default:
          return second.occupancy - first.occupancy;
      }
    });
  }

  // PORTFOLIO KPIs

  get totalProperties(): number {
    return this.pgs.length;
  }

  get activeProperties(): number {
    return this.pgs.filter(
      pg => this.isActive(pg.status)
    ).length;
  }

  get totalBeds(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.totalBeds || 0),
        0
      );
  }

  get occupiedBeds(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.occupiedBeds || 0),
        0
      );
  }

  get availableBeds(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.availableBeds || 0),
        0
      );
  }

  get totalActiveTenants(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.activeTenants || 0),
        0
      );
  }

  get overallOccupancy(): number {
    if (this.totalBeds <= 0) {
      return 0;
    }

    return Number(
      ((this.occupiedBeds / this.totalBeds) * 100).toFixed(1)
    );
  }

  get totalRentCollected(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.rentCollected || 0),
        0
      );
  }

  get totalRentDue(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.rentDue || 0),
        0
      );
  }

  get totalRentOverdue(): number {
    return Object.values(this.dashboardByPg)
      .reduce(
        (sum, item) => sum + Number(item.rentOverdue || 0),
        0
      );
  }

  get collectionPercentage(): number {
    const total = this.totalRentCollected + this.totalRentDue;

    if (total <= 0) {
      return 0;
    }

    return Number(
      ((this.totalRentCollected / total) * 100).toFixed(1)
    );
  }

  // METRICS FOR ONE PROPERTY

  getMetrics(pgId: number) {
    const item = this.dashboardByPg[pgId];

    if (!item) {
      return {
        rooms: 0,
        beds: 0,
        occupiedBeds: 0,
        availableBeds: 0,
        activeTenants: 0,
        occupancy: 0,
        rentCollected: 0,
        rentDue: 0,
        rentOverdue: 0
      };
    }

    return {
      rooms: Number(item.totalRooms || 0),
      beds: Number(item.totalBeds || 0),
      occupiedBeds: Number(item.occupiedBeds || 0),
      availableBeds: Number(item.availableBeds || 0),
      activeTenants: Number(item.activeTenants || 0),
      occupancy: Number(item.occupancyPercentage || 0),
      rentCollected: Number(item.rentCollected || 0),
      rentDue: Number(item.rentDue || 0),
      rentOverdue: Number(item.rentOverdue || 0)
    };
  }

  // FORMATTING AND STATUS

  occupancyWidth(value: number): number {
    return Math.max(0, Math.min(100, Number(value || 0)));
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(Number(amount || 0));
  }

  formatPercent(value: number): string {
    return `${Number(value || 0).toFixed(1)}%`;
  }

  isActive(status?: string): boolean {
    return String(status ?? '').trim().toUpperCase() === 'ACTIVE';
  }

  statusClass(status?: string): string {
    return this.isActive(status)
      ? 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200'
      : 'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-200';
  }

  typeClass(type?: string): string {
    const value = String(type ?? '').toLowerCase();

    if (value.includes('ladies')) {
      return 'bg-fuchsia-50 text-fuchsia-700 ring-1 ring-inset ring-fuchsia-200';
    }

    if (value.includes('gents')) {
      return 'bg-sky-50 text-sky-700 ring-1 ring-inset ring-sky-200';
    }

    return 'bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-200';
  }

  // FILTER AND VIEW ACTIONS

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedType = 'ALL';
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode = mode;
  }

  // ADD PROPERTY MODAL

  openAddModal(): void {
    this.saveError = '';

    this.addPgForm.reset({
      name: '',
      type: 'COLIVING',
      description: ''
    });

    this.isAddModalOpen = true;
  }

  closeAddModal(): void {
    if (this.saving) {
      return;
    }

    this.isAddModalOpen = false;
    this.saveError = '';
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeAddModal();
    }
  }

  // CREATE PROPERTY

  createProperty(): void {
    if (this.addPgForm.invalid || this.saving) {
      this.addPgForm.markAllAsTouched();
      return;
    }

    const value = this.addPgForm.getRawValue();

    const request: CreatePgRequest = {
      name: value.name!.trim(),
      type: value.type!.trim(),
      description: value.description?.trim() || null
    };

    this.saving = true;
    this.saveError = '';

    this.pgService.createPg(request)
      .pipe(
        timeout({ first: 15000 }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe({
        next: (created) => {
          this.pgs = [created, ...this.pgs];
          this.isAddModalOpen = false;

          this.showToast('PG property created successfully.');

          // Refresh metrics for the updated portfolio.
          this.loadDashboardMetrics();
        },

        error: (error: unknown) => {
          console.error(
            '[PG LIST] Create property failed:',
            error
          );

          this.saveError = this.getErrorMessage(
            error,
            'Unable to create the PG property. Please try again.'
          );
        }
      });
  }

  // NAVIGATION

  openRooms(pgId: number): void {
    void this.router.navigate(['/rooms'], {
      queryParams: { pgId }
    });
  }

  openTenants(pgId: number): void {
    void this.router.navigate(['/tenants'], {
      queryParams: { pgId }
    });
  }

  openRent(): void {
    void this.router.navigate(['/rent']);
  }

  openNotifications(): void {
    void this.router.navigate(['/notifications']);
  }

  // OTHER ACTIONS

  printPortfolio(): void {
    window.print();
  }

  launchBranchOnboarding(): void {
    this.openAddModal();
  }

  reload(): void {
    this.loadProperties();
  }

  // TOAST MESSAGE

  private showToast(message: string): void {
    this.toastMessage = message;

    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }

    this.toastTimer = setTimeout(() => {
      this.toastMessage = '';
      this.toastTimer = undefined;
    }, 3500);
  }

  trackById(_index: number, pg: PgProperty): number {
    return pg.id;
  }

  // ERROR HANDLING

  private getErrorMessage(
    error: unknown,
    fallback: string
  ): string {
    if (error instanceof TimeoutError) {
      return 'The request timed out. Please try again.';
    }

    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Cannot connect to Spring Boot. Check that the backend is running.';
      }

      if (error.status === 401) {
        return 'Your session has expired. Please log in again.';
      }

      if (error.status === 403) {
        return 'Access denied. Please check your account permissions.';
      }

      const message = error.error?.message || error.error?.error;

      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }

    return fallback;
  }
}
