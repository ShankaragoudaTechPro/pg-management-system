import { timeout } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Tenant, TenantSummary } from './tenant.model';
import { TenantService } from './tenant.service';
import { PgService } from '../../core/services/pg.service';
import { PgProperty } from '../../core/models/pg.model';
import { RoomService } from '../rooms/room.service';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenants.html',
  styleUrl: './tenants.scss'
})
export class TenantsComponent implements OnInit {
  private readonly tenantService = inject(TenantService);
  private readonly pgService = inject(PgService);
  private readonly roomService = inject(RoomService);

  tenants: Tenant[] = [];
  pgOptions: PgProperty[] = [];
  roomOptions: TenantRoomOption[] = [];
  bedOptions: TenantBedOption[] = [];
  loadingRooms = false;
  loadingPgs = false;
  loading = false;
  saving = false;
  showForm = false;
  editingId: number | null = null;

  searchTerm = '';
  selectedStatus = 'ALL';
  selectedPg = 'ALL';

  errorMessage = '';
  successMessage = '';

  form: Tenant = this.emptyTenant();

  private emptyTenant(): Tenant {
    return {
      fullName: '',
      mobileNumber: '',
      email: '',
      aadhaarNumber: '',
      gender: '',
      joiningDate: new Date().toISOString().slice(0, 10),
      monthlyRent: 6500,
      securityDeposit: 13000,
      status: 'ACTIVE',
      pgName: '',
      roomNumber: '',
      bedId: undefined,
      bedNumber: '',
      emergencyContact: '',
      emergencyContactName: ''
    };
  }

  ngOnInit(): void {
    this.loadTenants();
    this.loadPgOptions();
  }

  private loadPgOptions(): void {
    this.loadingPgs = true;
    this.pgService.getMyPgs().pipe(timeout({ first: 10000 })).subscribe({
      next: pgs => {
        this.pgOptions = pgs ?? [];
        this.loadingPgs = false;
      },
      error: error => {
        console.error('Failed to load PG options:', error);
        this.loadingPgs = false;
        this.errorMessage = 'Unable to load PG properties. Create a PG first, then try adding a tenant.';
      }
    });
  }

  onPgChange(pgId: number | string | null | undefined): void {
    const id = Number(pgId);
    this.form.pgId = Number.isInteger(id) && id > 0 ? id : undefined;
    this.form.roomId = undefined;
    this.form.bedId = undefined;
    this.form.roomNumber = '';
    this.form.bedNumber = '';
    this.roomOptions = [];
    this.bedOptions = [];
    if (!this.form.pgId) return;
    this.loadRoomOptions(this.form.pgId);
  }

  private loadRoomOptions(pgId: number, preferredRoomId?: number): void {
    this.loadingRooms = true;
    this.roomService.getRooms(pgId).pipe(timeout({ first: 10000 })).subscribe({
      next: rooms => {
        this.roomOptions = (rooms ?? []).map(room => {
          const raw = room as unknown as {
            id?: number; roomNumber?: string; active?: boolean;
            beds?: Array<{ id?: number; bedNumber?: number | string; status?: string }>;
          };
          return {
            id: Number(raw.id),
            roomNumber: raw.roomNumber ?? '',
            active: raw.active !== false,
            beds: (raw.beds ?? []).map(bed => ({
              id: Number(bed.id),
              bedNumber: String(bed.bedNumber ?? ''),
              status: String(bed.status ?? 'AVAILABLE').toUpperCase()
            }))
          };
        }).filter(room => room.id > 0 && room.active);

        this.loadingRooms = false;
        const roomId = preferredRoomId ?? this.form.roomId;
        if (roomId && this.roomOptions.some(room => room.id === Number(roomId))) {
          this.onRoomChange(Number(roomId), true);
        }
      },
      error: error => {
        console.error('Failed to load rooms:', error);
        this.loadingRooms = false;
        this.roomOptions = [];
        this.bedOptions = [];
        this.errorMessage = 'Unable to load rooms for the selected PG.';
      }
    });
  }

  onRoomChange(roomId: number | string | null | undefined, preserveBed = false): void {
    const id = Number(roomId);
    this.form.roomId = Number.isInteger(id) && id > 0 ? id : undefined;
    this.form.roomNumber = this.roomOptions.find(room => room.id === id)?.roomNumber ?? '';
    if (!preserveBed) {
      this.form.bedId = undefined;
      this.form.bedNumber = '';
    }

    const room = this.roomOptions.find(item => item.id === id);
    const currentBedId = preserveBed ? this.form.bedId : undefined;
    this.bedOptions = (room?.beds ?? []).filter(bed =>
      bed.status === 'AVAILABLE' || bed.id === currentBedId
    );
  }

  onBedChange(bedId: number | string | null | undefined): void {
    const id = Number(bedId);
    this.form.bedId = Number.isInteger(id) && id > 0 ? id : undefined;
    this.form.bedNumber = this.bedOptions.find(bed => bed.id === id)?.bedNumber ?? '';
  }

  loadTenants(): void {
    this.loading = true;
    this.errorMessage = '';

    this.tenantService.getTenants().pipe(timeout({ first: 10000 })).subscribe({
      next: data => {
        this.tenants = data ?? [];
        this.loading = false;
      },
      error: error => {
        this.errorMessage = error?.name === 'TimeoutError'
          ? 'Tenant request timed out after 10 seconds. Please try again.'
          : error?.status === 0
            ? 'Cannot connect to the backend. Check that Spring Boot is running.'
            : error?.status === 401
              ? 'Your session has expired. Please sign in again.'
              : error?.status === 403
                ? 'You do not have permission to view tenants.'
                : error?.error?.message ?? 'Unable to load tenants. Please try again.';
        this.loading = false;
      }
    });
  }

  get filteredTenants(): Tenant[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.tenants.filter(tenant => {
      const matchesSearch =
        !term ||
        tenant.fullName.toLowerCase().includes(term) ||
        tenant.mobileNumber.toLowerCase().includes(term) ||
        (tenant.email ?? '').toLowerCase().includes(term) ||
        (tenant.roomNumber ?? '').toLowerCase().includes(term);

      const matchesStatus =
        this.selectedStatus === 'ALL' ||
        tenant.status === this.selectedStatus;

      const matchesPg =
        this.selectedPg === 'ALL' ||
        String(tenant.pgId ?? tenant.pgName ?? '') === this.selectedPg;

      return matchesSearch && matchesStatus && matchesPg;
    });
  }

  get tenantPgFilterOptions(): { value: string; label: string }[] {
    const unique = new Map<string, string>();

    this.tenants.forEach(tenant => {
      const value = tenant.pgId != null
        ? String(tenant.pgId)
        : tenant.pgName ?? '';

      if (value) {
        unique.set(value, tenant.pgName || `PG ${value}`);
      }
    });

    return [...unique.entries()].map(([value, label]) => ({
      value,
      label
    }));
  }

  get summary(): TenantSummary {
    const visible = this.filteredTenants;

    return {
      total: this.tenants.length,
      active: visible.filter(t => t.status === 'ACTIVE').length,
      inactive: visible.filter(t => t.status === 'INACTIVE').length,
      monthlyRent: visible.reduce(
        (sum, tenant) => sum + Number(tenant.monthlyRent || 0), 0
      )
    };
  }

  initials(name: string): string {
    return name.trim().split(/\s+/).slice(0, 2)
      .map(part => part.charAt(0).toUpperCase()).join('');
  }

  openAddForm(): void {
    this.editingId = null;
    this.form = this.emptyTenant();
    this.roomOptions = [];
    this.bedOptions = [];
    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
  }

  openEditForm(tenant: Tenant): void {
    if (tenant.id == null) {
      this.errorMessage = 'Tenant ID is missing.';
      return;
    }

    this.editingId = tenant.id;
    this.form = {
      ...this.emptyTenant(),
      ...tenant,
      aadhaarNumber: tenant.aadhaarNumber ?? ''
    };
    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
    if (this.form.pgId) {
      this.loadRoomOptions(this.form.pgId, this.form.roomId);
    }
  }

  closeForm(): void {
    this.showForm = false;
    this.saving = false;
  }

  saveTenant(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (
      !this.form.fullName.trim() ||
      !/^[0-9]{10}$/.test(this.form.mobileNumber.trim()) ||
      !this.form.joiningDate ||
      !this.form.pgId ||
      !this.form.roomId ||
      !this.form.bedId ||
      !Number.isFinite(Number(this.form.monthlyRent)) ||
      this.form.monthlyRent <= 0 ||
      !Number.isFinite(Number(this.form.securityDeposit)) ||
      this.form.securityDeposit < 0
    ) {
      this.errorMessage =
        'Enter tenant details, select a PG, room and available bed, and provide valid rent details.';
      return;
    }

    const duplicate = this.tenants.some(tenant =>
      tenant.mobileNumber === this.form.mobileNumber.trim() &&
      tenant.id !== this.editingId
    );

    if (duplicate) {
      this.errorMessage = 'A tenant with this mobile number already exists.';
      return;
    }

    const wasEditing = this.editingId !== null;

    const payload: Tenant = {
      ...this.form,
      fullName: this.form.fullName.trim(),
      mobileNumber: this.form.mobileNumber.trim(),
      email: this.form.email?.trim() || undefined,
      aadhaarNumber: this.form.aadhaarNumber?.trim() || undefined
    };

    this.saving = true;

    const request = wasEditing
      ? this.tenantService.updateTenant(this.editingId!, payload)
      : this.tenantService.createTenant(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.successMessage = wasEditing
          ? 'Tenant updated successfully.'
          : 'Tenant added successfully.';
        this.loadTenants();
      },
      error: error => {
        this.saving = false;
        this.errorMessage =
          error?.error?.message ?? 'Unable to save tenant.';
      }
    });
  }

  deleteTenant(tenant: Tenant): void {
    if (tenant.id == null) {
      this.errorMessage = 'Tenant ID is missing.';
      return;
    }

    if (!confirm(`Remove tenant ${tenant.fullName}?`)) return;

    this.errorMessage = '';
    this.successMessage = '';

    this.tenantService.deleteTenant(tenant.id).subscribe({
      next: () => {
        this.tenants = this.tenants.filter(t => t.id !== tenant.id);
        this.successMessage = 'Tenant removed successfully.';
      },
      error: error => {
        this.errorMessage = error?.error?.message ??
          'Unable to remove tenant. Check active room or rent allocations.';
      }
    });
  }

  trackByTenantId(index: number, tenant: Tenant): number | string {
    return tenant.id ?? tenant.mobileNumber ?? index;
  }
}


interface TenantBedOption {
  id: number;
  bedNumber: string;
  status: string;
}

interface TenantRoomOption {
  id: number;
  roomNumber: string;
  active: boolean;
  beds: TenantBedOption[];
}
