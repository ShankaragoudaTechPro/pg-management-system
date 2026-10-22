import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../core/services/api-config';
import { OwnerProfile, OwnerProfileUpdate, UpdatePasswordRequest } from './profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_CONFIG.baseUrl.replace(/\/+$/, '')}/owners/me`;

  getProfile(): Observable<OwnerProfile> {
    return this.http.get<OwnerProfile>(this.baseUrl);
  }

  updateProfile(payload: OwnerProfileUpdate): Observable<OwnerProfile> {
    return this.http.put<OwnerProfile>(this.baseUrl, payload);
  }

  changePassword(payload: UpdatePasswordRequest): Observable<string> {
    return this.http.post(this.baseUrl + '/change-password', payload, {
      responseType: 'text'
    });
  }
}
