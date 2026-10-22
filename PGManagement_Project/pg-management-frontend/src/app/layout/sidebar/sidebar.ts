
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

type AppRole = 'OWNER' | 'SUPER_MAINTENANCE';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class SidebarComponent implements OnInit {

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  currentRole: AppRole = 'OWNER';

  menuItems: MenuItem[] = [];

  ngOnInit(): void {
    this.currentRole = this.readRoleFromToken();
    this.buildMenu();
  }

  get isSuperMaintenance(): boolean {
    return this.currentRole === 'SUPER_MAINTENANCE';
  }

  private readRoleFromToken(): AppRole {
    if (!isPlatformBrowser(this.platformId)) {
      return 'OWNER';
    }

    try {
      const token =
        localStorage.getItem('token') ??
        localStorage.getItem('accessToken') ??
        localStorage.getItem('jwtToken');

      if (!token) {
        return 'OWNER';
      }

      const parts = token.split('.');

      if (parts.length !== 3) {
        return 'OWNER';
      }

      const base64Payload = parts[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const decodedPayload = atob(base64Payload);
      const payload = JSON.parse(decodedPayload) as {
        role?: string;
      };

      // UI display only. Backend authorization remains authoritative.
      return payload.role === 'SUPER_MAINTENANCE'
        ? 'SUPER_MAINTENANCE'
        : 'OWNER';

    } catch {
      return 'OWNER';
    }
  }

  private buildMenu(): void {
    const ownerMenu: MenuItem[] = [
      { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
      { label: 'My PGs', icon: 'building', route: '/pgs' },
      { label: 'Rooms & Beds', icon: 'beds', route: '/rooms' },
      { label: 'Tenants', icon: 'users', route: '/tenants' },
      { label: 'Rent', icon: 'wallet', route: '/rent' },
      { label: 'Notifications', icon: 'bell', route: '/notifications' },
      { label: 'Profile', icon: 'profile', route: '/profile' }
    ];

    const maintenanceMenu: MenuItem[] = [
      { label: 'Operations', icon: 'dashboard', route: '/dashboard' },
      { label: 'PG Operations', icon: 'building', route: '/pgs' },
      { label: 'Rooms & Beds', icon: 'beds', route: '/rooms' },
      { label: 'Tenant Operations', icon: 'users', route: '/tenants' },
      { label: 'Notifications', icon: 'bell', route: '/notifications' },
      {
        label: 'Account Control',
        icon: 'shield',
        route: '/maintenance-control'
      }
    ];

    this.menuItems = this.isSuperMaintenance
      ? maintenanceMenu
      : ownerMenu;
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
