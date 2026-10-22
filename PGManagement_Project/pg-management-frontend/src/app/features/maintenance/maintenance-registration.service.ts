

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


import { API_CONFIG } from '../../core/services/api-config';

export interface MaintenanceStaffRegistrationRequest {
  name: string;
  mobile: string;
  email: string;
  password: string;
}

export interface MaintenanceStaffRegistrationResponse {
  id: number;
  name: string;
  email: string;
  mobile: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class MaintenanceRegistrationService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    `${API_CONFIG.baseUrl}/maintenance/auth/register`;

  register(
    request: MaintenanceStaffRegistrationRequest
  ): Observable<MaintenanceStaffRegistrationResponse> {
    return this.http.post<MaintenanceStaffRegistrationResponse>(
      this.apiUrl,
      request
    );
  }
}