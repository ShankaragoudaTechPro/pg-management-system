
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_CONFIG } from './api-config';

export interface MaintenanceLoginRequest {
  email: string;
  password: string;
}

export interface MaintenanceLoginResponse {
  token: string;
  tokenType: string;
  staffId: number;
  name: string;
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class MaintenanceAuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly baseUrl =
    `${API_CONFIG.baseUrl}/maintenance/auth`;

  private readonly tokenKey = 'maintenance_token';
  private readonly userKey = 'maintenance_user';

  login(
    request: MaintenanceLoginRequest
  ): Observable<MaintenanceLoginResponse> {
    return this.http.post<MaintenanceLoginResponse>(
      `${this.baseUrl}/login`,
      {
        email: request.email.trim().toLowerCase(),
        password: request.password
      }
    ).pipe(
      tap(response => {
        if (!isPlatformBrowser(this.platformId)) {
          return;
        }

        if (
          !response?.token ||
          response.role !== 'SUPER_MAINTENANCE'
        ) {
          throw new Error(
            'Invalid maintenance authentication response'
          );
        }

        localStorage.setItem(this.tokenKey, response.token);
        localStorage.setItem(
          this.userKey,
          JSON.stringify({
            staffId: response.staffId,
            name: response.name,
            email: response.email,
            role: response.role
          })
        );
      })
    );
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    return localStorage.getItem(this.tokenKey);
  }

  getUser(): {
    staffId: number;
    name: string;
    email: string;
    role: string;
  } | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    const value = localStorage.getItem(this.userKey);

    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value);
    } catch {
      return null;
    }
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.tokenKey);
      localStorage.removeItem(this.userKey);
    }
  }
}
