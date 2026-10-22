import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, debounceTime, filter, fromEvent, interval, finalize, timeout } from 'rxjs';

import { DashboardService } from '../../../core/services/dashboard.service';
import { DashboardResponse } from '../../../core/models/dashboard.model';
import { DashboardSyncService } from '../../../core/services/dashboard-sync.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly router = inject(Router);
  private readonly dashboardSync = inject(DashboardSyncService);
  private dashboardRequest?: Subscription;
  private refreshTimer?: Subscription;
  private syncSubscription?: Subscription;
  private focusSubscription?: Subscription;
  private visibilitySubscription?: Subscription;
  private requestVersion = 0;

  dashboards: DashboardResponse[] = [];
  selectedPg: DashboardResponse | null = null;
  loading = true;
  refreshing = false;
  errorMessage = '';
  lastUpdatedAt: Date | null = null;

  ngOnInit(): void {
    this.loadDashboard();
    this.syncSubscription = this.dashboardSync.changes$
      .pipe(debounceTime(100))
      .subscribe(() => this.loadDashboard(true));

    // Browser-only listeners keep data fresh after returning to this tab.
    if (typeof window !== 'undefined' && typeof document !== 'undefined') {
      this.refreshTimer = interval(60_000).subscribe(() => {
        if (document.visibilityState === 'visible' && !this.refreshing) {
          this.loadDashboard(true);
        }
      });
      this.focusSubscription = fromEvent(window, 'focus').subscribe(() => this.refreshIfStale());
      this.visibilitySubscription = fromEvent(document, 'visibilitychange')
        .pipe(filter(() => document.visibilityState === 'visible'))
        .subscribe(() => this.refreshIfStale());
    }
  }

  ngOnDestroy(): void {
    this.requestVersion++;
    this.dashboardRequest?.unsubscribe();
    this.refreshTimer?.unsubscribe();
    this.syncSubscription?.unsubscribe();
    this.focusSubscription?.unsubscribe();
    this.visibilitySubscription?.unsubscribe();
  }

  private refreshIfStale(): void {
    const lastUpdated = this.lastUpdatedAt?.getTime() ?? 0;
    if (!this.refreshing && Date.now() - lastUpdated > 15_000) {
      this.loadDashboard(true);
    }
  }

  loadDashboard(silent = false): void {
    const version = ++this.requestVersion;
    const previousPgId = this.selectedPg?.pgId;

    this.dashboardRequest?.unsubscribe();
    this.refreshing = true;
    if (this.dashboards.length === 0) this.loading = true;
    else if (!silent) this.loading = false;
    this.errorMessage = '';

    this.dashboardRequest = this.dashboardService.getDashboard().pipe(
      timeout({ first: 10000 }),
      finalize(() => {
        if (version === this.requestVersion) {
          this.loading = false;
          this.refreshing = false;
        }
      })
    ).subscribe({
      next: (response: DashboardResponse[]) => {
        if (version !== this.requestVersion) return;

        if (!Array.isArray(response)) {
          this.errorMessage = 'The dashboard API returned an unexpected response.';
          this.loading = false;
          return;
        }

        const valid = response.filter(pg =>
          pg &&
          Number.isFinite(Number(pg.pgId)) &&
          Number(pg.pgId) > 0
        );

        if (valid.length !== response.length) {
          this.errorMessage = 'Some dashboard records contain invalid property IDs.';
          this.loading = false;
          return;
        }

        this.dashboards = valid;
        this.selectedPg = valid.find(pg => pg.pgId === previousPgId) ?? valid[0] ?? null;
        this.lastUpdatedAt = new Date();
        this.loading = false;
        this.refreshing = false;
      },
      error: (error: HttpErrorResponse) => {
        if (version !== this.requestVersion) return;
        console.error('Dashboard API error:', error);
        this.errorMessage = this.getErrorMessage(error);
        this.loading = false;
      }
    });
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    const body = error.error;
    if (body && typeof body === 'object') {
      const message = body.message ?? body.detail ?? body.error;
      if (typeof message === 'string' && message.trim()) return message;
    }
    if (typeof body === 'string' && body.trim()) return body;
    if (error.status === 0) return 'Cannot connect to the backend. Check that Spring Boot is running.';
    if (error.status === 401) return 'Your session has expired. Please log in again.';
    if (error.status === 403) return 'Access denied. Please sign in with an active PG owner account.';
    if (error.status === 404) return 'Dashboard endpoint was not found. Check the backend API path.';
    if (error.status >= 500) return 'The server could not load dashboard data. Check the backend logs.';
    return 'Unable to load dashboard. Please try again.';
  }

  private toNumber(value: unknown): number {
    const parsed = Number(value ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private sum(selector: (pg: DashboardResponse) => unknown): number {
    return this.dashboards.reduce((total, pg) => total + this.toNumber(selector(pg)), 0);
  }

  selectPg(pg: DashboardResponse): void { this.selectedPg = pg; }
  refresh(): void { this.loadDashboard(true); }
  addPg(): void { void this.router.navigate(['/pgs/add']); }
  addFirstPg(): void { this.addPg(); }
  openPgs(): void { void this.router.navigate(['/pgs']); }
  openRooms(): void {
    if (this.selectedPg?.pgId) {
      void this.router.navigate(['/rooms'], { queryParams: { pgId: this.selectedPg.pgId } });
    } else {
      void this.router.navigate(['/pgs']);
    }
  }
  openTenants(): void { void this.router.navigate(['/tenants']); }
  openRent(): void { void this.router.navigate(['/rent']); }
  openNotifications(): void { void this.router.navigate(['/notifications']); }

  formatCurrency(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(this.toNumber(value));
  }
  formatPercent(value: number | null | undefined): string {
    return `${this.toNumber(value).toFixed(1)}%`;
  }
  occupancyWidth(value: number | null | undefined): number {
    return Math.max(0, Math.min(100, this.toNumber(value)));
  }

  get totalPgs(): number { return this.dashboards.length; }
  get totalRooms(): number { return this.sum(pg => pg.totalRooms); }
  get totalBeds(): number { return this.sum(pg => pg.totalBeds); }
  get totalOccupiedBeds(): number { return this.sum(pg => pg.occupiedBeds); }
  get totalAvailableBeds(): number { return this.sum(pg => pg.availableBeds); }
  get totalActiveTenants(): number { return this.sum(pg => pg.activeTenants); }
  get totalRentCollected(): number { return this.sum(pg => pg.rentCollected); }
  get totalRentDue(): number { return this.sum(pg => pg.rentDue); }
  get totalRentOverdue(): number { return this.sum(pg => pg.rentOverdue); }

  get totalOccupancyPercentage(): number {
    return this.totalBeds > 0
      ? Number(((this.totalOccupiedBeds / this.totalBeds) * 100).toFixed(1))
      : 0;
  }

  get selectedOccupancy(): number { return this.toNumber(this.selectedPg?.occupancyPercentage); }
  get selectedRooms(): number { return this.toNumber(this.selectedPg?.totalRooms); }
  get selectedBeds(): number { return this.toNumber(this.selectedPg?.totalBeds); }
  get selectedOccupiedBeds(): number { return this.toNumber(this.selectedPg?.occupiedBeds); }
  get selectedAvailableBeds(): number { return this.toNumber(this.selectedPg?.availableBeds); }
  get selectedActiveTenants(): number { return this.toNumber(this.selectedPg?.activeTenants); }
}
