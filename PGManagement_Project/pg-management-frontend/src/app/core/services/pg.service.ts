import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap, timeout } from 'rxjs';

import { API_CONFIG } from './api-config';
import { PgProperty } from '../models/pg.model';
import { DashboardSyncService } from './dashboard-sync.service';

export interface CreatePgRequest {
  name: string;
  type: string;
  description?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class PgService {

  private readonly http = inject(HttpClient);
  private readonly dashboardSync = inject(DashboardSyncService);

  // Normalize the URL to prevent accidental double slashes.
  private readonly baseUrl = API_CONFIG.baseUrl.replace(/\/+$/, '');

  // =====================================================
  // GET MY PG PROPERTIES
  // =====================================================

  getMyPgs(): Observable<PgProperty[]> {
    return this.http
      .get<unknown>(`${this.baseUrl}/pgs`)
      .pipe(
        timeout({ first: 8000 }),
        map(response => this.normalizePgResponse(response))
      );
  }

  // Support a direct array or common list response wrappers.
  private normalizePgResponse(response: unknown): PgProperty[] {
    if (Array.isArray(response)) {
      return response as PgProperty[];
    }

    if (response && typeof response === 'object') {
      const body = response as Record<string, unknown>;

      for (const key of [
        'data',
        'items',
        'content',
        'pgs',
        'results'
      ]) {
        const value = body[key];

        if (Array.isArray(value)) {
          return value as PgProperty[];
        }
      }
    }

    console.error(
      '[PG SERVICE] Unexpected properties API response:',
      response
    );

    throw new Error(
      'The PG properties API returned an unexpected response format.'
    );
  }

  // =====================================================
  // CREATE PG PROPERTY
  // =====================================================

  createPg(request: CreatePgRequest): Observable<PgProperty> {
    return this.http.post<PgProperty>(
      `${this.baseUrl}/pgs`,
      request
    ).pipe(tap(() => this.dashboardSync.notifyDataChanged()));
  }

  // =====================================================
  // GET PROPERTY BY ID
  // =====================================================

  getPgById(pgId: number): Observable<PgProperty> {
    return this.http.get<PgProperty>(
      `${this.baseUrl}/pgs/${pgId}`
    );
  }
}