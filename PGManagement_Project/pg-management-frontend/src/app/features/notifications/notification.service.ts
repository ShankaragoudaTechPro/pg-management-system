import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AppNotification } from './notification.model';
import { API_CONFIG } from '../../core/services/api-config';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${API_CONFIG.baseUrl}/notifications`;

  getAll(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.apiUrl);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/read`,
      {}
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch(
      `${this.apiUrl}/read-all`,
      {},
      { responseType: 'text' }
    ).pipe(map(() => undefined));
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete(
      `${this.apiUrl}/${id}`,
      { responseType: 'text' }
    ).pipe(map(() => undefined));
  }
}