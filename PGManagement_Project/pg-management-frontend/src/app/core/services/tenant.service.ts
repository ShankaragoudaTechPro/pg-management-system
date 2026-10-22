import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { API_CONFIG } from './api-config';
import {
  CreateTenantRequest,
  Tenant,
  TenantPage
} from '../models/tenant.model';
import { DashboardSyncService } from './dashboard-sync.service';

@Injectable({
  providedIn: 'root'
})
export class TenantService {

  private readonly http = inject(HttpClient);
  private readonly dashboardSync = inject(DashboardSyncService);
  private readonly baseUrl = API_CONFIG.baseUrl.replace(/\/+$/, '');

  getTenants(
    page: number = 0,
    size: number = 10,
    search: string = '',
    active: boolean | null = null,
    pgId: number | null = null
  ): Observable<TenantPage> {

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    if (active !== null) {
      params = params.set('active', active);
    }

    if (pgId !== null) {
      params = params.set('pgId', pgId);
    }

    return this.http.get<TenantPage>(
      `${this.baseUrl}/tenants`,
      { params }
    );
  }

  getTenantById(id: number): Observable<Tenant> {
    return this.http.get<Tenant>(
      `${this.baseUrl}/tenants/${id}`
    );
  }

  createTenant(
    request: CreateTenantRequest
  ): Observable<Tenant> {
    return this.http.post<Tenant>(
      `${this.baseUrl}/tenants`,
      request
    ).pipe(tap(() => this.dashboardSync.notifyDataChanged()));
  }

  checkoutTenant(id: number): Observable<string> {
    return this.http.patch(
      `${this.baseUrl}/tenants/${id}/checkout`,
      {},
      {
        responseType: 'text'
      }
    ).pipe(tap(() => this.dashboardSync.notifyDataChanged()));
  }
}