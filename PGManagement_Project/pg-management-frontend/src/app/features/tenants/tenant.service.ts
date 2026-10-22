import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { API_CONFIG } from '../../core/services/api-config';
import { Tenant } from './tenant.model';
import { DashboardSyncService } from '../../core/services/dashboard-sync.service';

export interface TenantWriteRequest {
  name: string;
  mobile: string;
  email?: string;
  aadhaarNumber?: string;
  gender: string;
  joiningDate: string;
  emergencyContact?: string;
  emergencyContactName?: string;
  pgId: number;
  roomId: number;
  bedId: number;
  monthlyRent: number;
  securityDeposit: number;
}

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly http = inject(HttpClient);
  private readonly dashboardSync = inject(DashboardSyncService);
  private readonly apiUrl = `${API_CONFIG.baseUrl}/tenants`;

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.apiUrl).pipe(
      map(items => (items ?? []).map(item => this.normalizeTenant(item)))
    );
  }

  getTenantById(id: number): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.apiUrl}/${id}`).pipe(
      map(item => this.normalizeTenant(item))
    );
  }

  createTenant(tenant: Tenant): Observable<Tenant> {
    return this.http.post<Tenant>(this.apiUrl, this.toRequest(tenant)).pipe(
      map(item => this.normalizeTenant(item)),
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  updateTenant(id: number, tenant: Tenant): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.apiUrl}/${id}`, this.toRequest(tenant)).pipe(
      map(item => this.normalizeTenant(item)),
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  deleteTenant(id: number): Observable<void> {
    // Tenant history must be preserved; backend DELETE performs a safe checkout.
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  private toRequest(tenant: Tenant): TenantWriteRequest {
    if (!tenant.pgId || !tenant.roomId || !tenant.bedId) {
      throw new Error('Select a PG, room, and available bed before saving the tenant.');
    }

    return {
      name: tenant.fullName.trim(),
      mobile: tenant.mobileNumber.trim(),
      email: tenant.email?.trim() || undefined,
      aadhaarNumber: tenant.aadhaarNumber?.trim() || undefined,
      gender: tenant.gender || 'OTHER',
      joiningDate: tenant.joiningDate,
      emergencyContact: tenant.emergencyContact?.trim() || undefined,
      emergencyContactName: tenant.emergencyContactName?.trim() || undefined,
      pgId: Number(tenant.pgId),
      roomId: Number(tenant.roomId),
      bedId: Number(tenant.bedId),
      monthlyRent: Number(tenant.monthlyRent),
      securityDeposit: Number(tenant.securityDeposit)
    };
  }

  private normalizeTenant(value: Tenant): Tenant {
    const raw = value as Tenant & {
      name?: string;
      mobile?: string;
      active?: boolean;
    };
    return {
      ...raw,
      fullName: raw.fullName || raw.name || '',
      mobileNumber: raw.mobileNumber || raw.mobile || '',
      status: raw.status || (raw.active === false ? 'INACTIVE' : 'ACTIVE'),
      bedNumber: raw.bedNumber == null ? '' : String(raw.bedNumber)
    };
  }
}
