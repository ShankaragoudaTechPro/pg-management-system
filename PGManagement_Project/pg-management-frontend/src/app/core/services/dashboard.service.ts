import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';

import { API_CONFIG } from './api-config';
import { DashboardResponse } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardResponse[]> {
    return this.http.get<DashboardResponse[]>(
      `${API_CONFIG.baseUrl.replace(/\/+$/, '')}/dashboard`
    ).pipe(timeout({ first: 10000 }));
  }

  getPgDashboard(pgId: number): Observable<DashboardResponse> {
    if (!Number.isInteger(pgId) || pgId <= 0) {
      throw new Error('A valid PG ID is required to load its dashboard.');
    }

    return this.http.get<DashboardResponse>(
      `${API_CONFIG.baseUrl.replace(/\/+$/, '')}/dashboard/pg/${pgId}`
    ).pipe(timeout({ first: 10000 }));
  }
}