import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { Room } from './room.model';
import { API_CONFIG } from '../../core/services/api-config';
import { DashboardSyncService } from '../../core/services/dashboard-sync.service';

export interface RoomRequest {
  roomNumber: string;
  sharing: number;
  floor: number;
}

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private readonly http = inject(HttpClient);
  private readonly dashboardSync = inject(DashboardSyncService);
  private readonly baseUrl = `${API_CONFIG.baseUrl}/pgs`;

  private getRoomsUrl(pgId: number): string {
    if (!Number.isInteger(pgId) || pgId <= 0) {
      throw new Error('A valid PG ID is required.');
    }

    return `${this.baseUrl}/${pgId}/rooms`;
  }

  getRooms(pgId: number): Observable<Room[]> {
    return this.http.get<Room[]>(this.getRoomsUrl(pgId));
  }

  createRoom(pgId: number, room: RoomRequest): Observable<Room> {
    return this.http.post<Room>(this.getRoomsUrl(pgId), room).pipe(
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }

  updateRoom(
    pgId: number,
    roomId: number,
    room: RoomRequest
  ): Observable<Room> {
    if (!Number.isInteger(roomId) || roomId <= 0) {
      throw new Error('A valid room ID is required.');
    }

    return this.http.put<Room>(
      `${this.getRoomsUrl(pgId)}/${roomId}`,
      room
    ).pipe(tap(() => this.dashboardSync.notifyDataChanged()));
  }

  deactivateRoom(pgId: number, roomId: number): Observable<void> {
    if (!Number.isInteger(roomId) || roomId <= 0) {
      throw new Error('A valid room ID is required.');
    }

    return this.http.patch(
      `${this.getRoomsUrl(pgId)}/${roomId}/deactivate`,
      {},
      { responseType: 'text' }
    ).pipe(
      map(() => undefined),
      tap(() => this.dashboardSync.notifyDataChanged())
    );
  }
}