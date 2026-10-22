import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

import { API_CONFIG } from '../../core/services/api-config';
import { DashboardSyncService } from '../../core/services/dashboard-sync.service';
import {
  RentRecord,
  RentCreateRequest,
  RecordPaymentRequest
} from './rent.model';

interface RentApiResponse {
  id: number;
  tenantId: number;
  tenantName: string;
  pgId?: number;
  pgName?: string;
  rentAmount: number;
  periodStart: string;
  periodEnd: string;
  dueDate: string;
  paidDate?: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
  paymentMode?: RentRecord['paymentMode'];
  paymentNote?: string;
}

interface RentApiPageResponse {
  content: RentApiResponse[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export interface RentListResponse {
  content: RentRecord[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export type RentListResult = RentRecord[] | RentListResponse;
type RentApiListResult = RentApiResponse[] | RentApiPageResponse;

export interface RentFilters {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  pgId?: number;
}

@Injectable({ providedIn: 'root' })
export class RentService {
  private readonly http = inject(HttpClient);
  private readonly dashboardSync = inject(DashboardSyncService);
  private readonly apiUrl = `${API_CONFIG.baseUrl.replace(/\/+$/, '')}/rents`;

  getAll(filters?: RentFilters): Observable<RentListResult> {
    let params = new HttpParams();
    if (filters?.page !== undefined) params = params.set('page', filters.page);
    if (filters?.size !== undefined) params = params.set('size', filters.size);
    if (filters?.search) params = params.set('search', filters.search);
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters?.pgId !== undefined) params = params.set('pgId', filters.pgId);

    return this.http.get<RentApiListResult>(this.apiUrl, { params }).pipe(
      map(response => Array.isArray(response)
        ? response.map(item => this.normalize(item))
        : {
            ...response,
            content: (response.content ?? []).map(item => this.normalize(item))
          })
    );
  }

  getByTenant(tenantId: number): Observable<RentRecord[]> {
    return this.http.get<RentApiResponse[]>(`${this.apiUrl}/tenant/${tenantId}`).pipe(
      map(items => (items ?? []).map(item => this.normalize(item)))
    );
  }

  getById(rentId: number): Observable<RentRecord> {
    return this.http.get<RentApiResponse>(`${this.apiUrl}/${rentId}`).pipe(
      map(item => this.normalize(item))
    );
  }

  createForTenant(tenantId: number, payload: RentCreateRequest): Observable<RentRecord> {
    return this.http.post<RentApiResponse>(`${this.apiUrl}/tenant/${tenantId}`, payload).pipe(
      map(item => this.normalize(item)),
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  recordPayment(rentId: number, payload: RecordPaymentRequest): Observable<RentRecord> {
    return this.http.patch<RentApiResponse>(`${this.apiUrl}/${rentId}/paid`, payload).pipe(
      map(item => this.normalize(item)),
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  private normalize(item: RentApiResponse): RentRecord {
    const amount = Number(item.rentAmount ?? 0);
    const status = item.status;
    return {
      id: item.id,
      tenantId: item.tenantId,
      tenantName: item.tenantName ?? '',
      pgId: item.pgId,
      pgName: item.pgName,
      periodStart: item.periodStart,
      periodEnd: item.periodEnd,
      rentMonth: item.periodStart?.slice(0, 7) ?? '',
      amount,
      paidAmount: status === 'PAID' ? amount : 0,
      dueDate: item.dueDate,
      paidDate: item.paidDate,
      status,
      paymentMode: item.paymentMode,
      note: item.paymentNote
    };
  }
}
