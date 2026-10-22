import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  finalize,
  timeout,
  TimeoutError
} from 'rxjs';

import { Room, RoomSummary } from './room.model';
import { RoomRequest, RoomService } from './room.service';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rooms.html',
  styleUrl: './rooms.scss'
})
export class RoomsComponent implements OnInit {
  private readonly roomService = inject(RoomService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  pgId: number | null = null;
  rooms: Room[] = [];

  loading = false;
  saving = false;
  deletingId: number | null = null;

  showForm = false;
  editingId: number | null = null;

  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  selectedFloor = 'ALL';
  selectedSharing = 'ALL';
  selectedStatus = 'ALL';

  form: Room = this.emptyRoom();

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const rawPgId = params.get('pgId');
      const id = Number(rawPgId);

      if (!rawPgId || !Number.isInteger(id) || id <= 0) {
        this.pgId = null;
        this.rooms = [];
        this.errorMessage =
          'Select a PG property to manage its rooms and beds.';

        void this.router.navigate(['/pgs'], {
          queryParams: { returnTo: 'rooms' },
          replaceUrl: true
        });

        return;
      }

      if (this.pgId !== id) {
        this.pgId = id;
        this.loadRooms();
      }
    });
  }

  private emptyRoom(): Room {
    return {
      roomNumber: '',
      floorNumber: 1,
      sharingType: 'DOUBLE',
      totalBeds: 2,
      status: 'ACTIVE',
      beds: []
    };
  }

  private normalizeRoom(room: Room): Room {
    const sharingMap: Record<string, Room['sharingType']> = {
      '1': 'SINGLE',
      '2': 'DOUBLE',
      '3': 'TRIPLE',
      '4': 'FOUR_SHARING',
      SINGLE: 'SINGLE',
      DOUBLE: 'DOUBLE',
      TRIPLE: 'TRIPLE',
      FOUR: 'FOUR_SHARING',
      FOUR_SHARING: 'FOUR_SHARING'
    };

    const rawRoom = room as Room & {
      sharing?: number | string;
      floor?: number | string;
      active?: boolean;
    };

    const sharingValue = String(
      rawRoom.sharingType ?? rawRoom.sharing ?? 'DOUBLE'
    ).trim().toUpperCase();

    const beds = room.beds ?? [];

    const rawFloor = Number(
      rawRoom.floorNumber ?? rawRoom.floor ?? 1
    );

    return {
      ...room,
      roomNumber: String(room.roomNumber ?? ''),
      floorNumber: Number.isFinite(rawFloor) ? rawFloor : 1,
      sharingType: sharingMap[sharingValue] ?? 'DOUBLE',
      totalBeds: beds.filter(
        bed => bed.status !== 'INACTIVE'
      ).length,
      status: rawRoom.active === false ? 'INACTIVE' : 'ACTIVE',
      beds
    };
  }

  loadRooms(): void {
    if (this.pgId === null) {
      this.errorMessage = 'A valid PG ID is required.';
      return;
    }

    const currentPgId = this.pgId;

    this.loading = true;
    this.errorMessage = '';

    this.roomService.getRooms(currentPgId)
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (data: Room[]) => {
          if (this.pgId !== currentPgId) {
            return;
          }

          this.rooms = (Array.isArray(data) ? data : [])
            .map(room => this.normalizeRoom(room));
        },

        error: (error: unknown) => {
          console.error('Load rooms failed:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to load rooms. Please try again.'
          );
        }
      });
  }

  refreshRooms(): void {
    if (!this.loading) {
      this.loadRooms();
    }
  }

  get floors(): number[] {
    return [...new Set(
      this.rooms.map(room => room.floorNumber)
    )].sort((a, b) => a - b);
  }

  get filteredRooms(): Room[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.rooms.filter(room => {
      const matchesSearch =
        !term ||
        room.roomNumber.toLowerCase().includes(term) ||
        (room.beds ?? []).some(bed =>
          (bed.tenantName ?? '').toLowerCase().includes(term)
        );

      const matchesFloor =
        this.selectedFloor === 'ALL' ||
        room.floorNumber === Number(this.selectedFloor);

      const matchesSharing =
        this.selectedSharing === 'ALL' ||
        room.sharingType === this.selectedSharing;

      const matchesStatus =
        this.selectedStatus === 'ALL' ||
        room.status === this.selectedStatus;

      return (
        matchesSearch &&
        matchesFloor &&
        matchesSharing &&
        matchesStatus
      );
    });
  }

  get summary(): RoomSummary {
    return {
      totalRooms: this.activeRooms,

      totalBeds: this.rooms.reduce(
        (sum, room) =>
          sum + (
            room.status === 'ACTIVE'
              ? (room.beds ?? []).filter(
                  bed => bed.status !== 'INACTIVE'
                ).length
              : 0
          ),
        0
      ),

      occupiedBeds: this.rooms.reduce(
        (sum, room) =>
          sum + (
            room.status === 'ACTIVE'
              ? (room.beds ?? []).filter(
                  bed => bed.status === 'OCCUPIED'
                ).length
              : 0
          ),
        0
      ),

      availableBeds: this.rooms.reduce(
        (sum, room) =>
          sum + (
            room.status === 'ACTIVE'
              ? (room.beds ?? []).filter(
                  bed => bed.status === 'AVAILABLE'
                ).length
              : 0
          ),
        0
      ),

      maintenanceBeds: 0
    };
  }

  get activeRooms(): number {
    return this.rooms.filter(
      room => room.status === 'ACTIVE'
    ).length;
  }

  get inactiveRooms(): number {
    return this.rooms.filter(
      room => room.status === 'INACTIVE'
    ).length;
  }

  occupancyPercent(room: Room): number {
    if (!room.totalBeds) {
      return 0;
    }

    return Math.min(
      100,
      Math.round(
        this.occupiedCount(room) / room.totalBeds * 100
      )
    );
  }

  availableCount(room: Room): number {
    return (room.beds ?? []).filter(
      bed => bed.status === 'AVAILABLE'
    ).length;
  }

  occupiedCount(room: Room): number {
    return (room.beds ?? []).filter(
      bed => bed.status === 'OCCUPIED'
    ).length;
  }

  maintenanceCount(_room: Room): number {
    return 0;
  }

  sharingLabel(value: string): string {
    const labels: Record<string, string> = {
      SINGLE: 'Single Sharing',
      DOUBLE: 'Double Sharing',
      TRIPLE: 'Triple Sharing',
      FOUR_SHARING: 'Four Sharing'
    };

    return labels[value] ?? value.replace(/_/g, ' ');
  }

  statusLabel(value: string): string {
    return String(value ?? '').replace(/_/g, ' ');
  }

  openAddForm(): void {
    if (this.saving) {
      return;
    }

    if (this.pgId === null) {
      this.errorMessage = 'Open Rooms from a valid PG property.';
      return;
    }

    this.editingId = null;
    this.form = this.emptyRoom();
    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
  }

  openEditForm(room: Room): void {
    if (this.saving || this.deletingId !== null) {
      return;
    }

    if (room.id == null) {
      this.errorMessage = 'Room ID is missing; cannot edit this room.';
      return;
    }

    this.editingId = room.id;

    this.form = {
      ...this.emptyRoom(),
      ...room,
      beds: room.beds ?? []
    };

    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
  }

  closeForm(): void {
    if (this.saving) {
      return;
    }

    this.showForm = false;
    this.errorMessage = '';
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedFloor = 'ALL';
    this.selectedSharing = 'ALL';
    this.selectedStatus = 'ALL';
  }

  saveRoom(): void {
    if (this.saving || this.deletingId !== null) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    if (this.pgId === null) {
      this.errorMessage = 'A valid PG ID is required.';
      return;
    }

    const roomNumber = String(
      this.form.roomNumber ?? ''
    ).trim();

    const floor = Number(this.form.floorNumber);

    if (!roomNumber || !Number.isInteger(floor) || floor < 0) {
      this.errorMessage =
        'Enter a room number and a valid floor number.';
      return;
    }

    const duplicate = this.rooms.some(room =>
      room.roomNumber.toLowerCase() === roomNumber.toLowerCase() &&
      room.id !== this.editingId
    );

    if (duplicate) {
      this.errorMessage = 'This room number already exists.';
      return;
    }

    const sharingMap: Record<Room['sharingType'], number> = {
      SINGLE: 1,
      DOUBLE: 2,
      TRIPLE: 3,
      FOUR_SHARING: 4
    };

    const sharing = sharingMap[this.form.sharingType];

    if (!Number.isInteger(sharing)) {
      this.errorMessage = 'Please select a valid sharing type.';
      return;
    }

    const payload: RoomRequest = {
      roomNumber,
      sharing,
      floor
    };

    const currentPgId = this.pgId;
    const currentRoomId = this.editingId;
    const wasEditing = currentRoomId !== null;

    this.saving = true;

    const request = wasEditing
      ? this.roomService.updateRoom(
          currentPgId,
          currentRoomId!,
          payload
        )
      : this.roomService.createRoom(
          currentPgId,
          payload
        );

    request.pipe(
      timeout({ first: 10000 }),
      finalize(() => {
        this.saving = false;
      })
    ).subscribe({
      next: () => {
        this.showForm = false;

        this.successMessage = wasEditing
          ? 'Room updated successfully.'
          : 'Room created successfully.';

        this.loadRooms();
      },

      error: (error: unknown) => {
        console.error('Room save failed:', error);

        this.errorMessage = this.getErrorMessage(
          error,
          'Unable to save room. Please check the backend response.'
        );
      }
    });
  }

  deleteRoom(roomOrId: Room | number): void {
    if (this.deletingId !== null || this.saving) {
      return;
    }

    const room = typeof roomOrId === 'number'
      ? this.rooms.find(item => item.id === roomOrId)
      : roomOrId;

    if (this.pgId === null || !room || room.id == null) {
      this.errorMessage = 'A valid PG ID and room ID are required.';
      return;
    }

    if (!confirm(`Deactivate room ${room.roomNumber}?`)) {
      return;
    }

    const currentPgId = this.pgId;
    const roomId = room.id;

    this.errorMessage = '';
    this.successMessage = '';
    this.deletingId = roomId;

    this.roomService.deactivateRoom(currentPgId, roomId)
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.deletingId = null;
        })
      )
      .subscribe({
        next: () => {
          this.successMessage = 'Room deactivated successfully.';
          this.loadRooms();
        },

        error: (error: unknown) => {
          console.error('Room deactivation failed:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to deactivate room. It may have active tenants.'
          );
        }
      });
  }

  trackByRoomId(index: number, room: Room): number | string {
    return room.id ?? room.roomNumber ?? index;
  }

  private getErrorMessage(
    error: unknown,
    fallback: string
  ): string {
    if (error instanceof TimeoutError) {
      return 'Rooms request timed out after 10 seconds. Please try again.';
    }

    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Cannot connect to the backend. Check that Spring Boot is running.';
      }

      if (error.status === 401) {
        return 'Your session has expired. Please sign in again.';
      }

      if (error.status === 403) {
        return 'You do not have permission to perform this action.';
      }

      const backendMessage = error.error?.message;

      if (
        typeof backendMessage === 'string' &&
        backendMessage.trim()
      ) {
        return backendMessage;
      }
    }

    return fallback;
  }
}