
import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  PLATFORM_ID,
  inject
} from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class NavbarComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly platformId = inject(PLATFORM_ID);

  isProfileMenuOpen = false;

  get ownerEmail(): string {
    const token = this.authService.getToken();

    if (!token) {
      return 'User';
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || 'User';
    } catch {
      return 'User';
    }
  }

  get userInitial(): string {
    return this.ownerEmail.charAt(0).toUpperCase() || 'U';
  }

  get userRole(): string {
    const token = this.authService.getToken();

    if (!token) {
      return 'User';
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));

      return payload.role === 'SUPER_MAINTENANCE'
        ? 'Super Maintenance'
        : 'Owner / Admin';
    } catch {
      return 'User';
    }
  }

  get pageTitle(): string {
    const path = this.router.url.split('?')[0].split('#')[0];

    const titles: Record<string, string> = {
      '/dashboard': 'Dashboard',
      '/pgs': 'My PG Properties',
      '/rooms': 'Rooms & Beds',
      '/tenants': 'Tenant Management',
      '/rent': 'Rent Management',
      '/notifications': 'Notifications',
      '/profile': 'My Profile',
      '/maintenance-control': 'Account Control'
    };

    return titles[path] ?? 'PG Manager';
  }

  get pageSubtitle(): string {
    const subtitles: Record<string, string> = {
      '/dashboard': 'Your property operations at a glance',
      '/pgs': 'Manage your PG properties and facilities',
      '/rooms': 'Manage rooms, bed availability and occupancy',
      '/tenants': 'Manage tenant information and allocations',
      '/rent': 'Review rent and payment records',
      '/notifications': 'Stay updated on important activities',
      '/profile': 'Manage your account and preferences',
      '/maintenance-control':
        'Manage authorized account security actions'
    };

    const path = this.router.url.split('?')[0].split('#')[0];
    return subtitles[path] ?? 'Property Management Platform';
  }

  get isMobileBrandVisible(): boolean {
    return true;
  }

  goToProfile(): void {
    this.isProfileMenuOpen = false;
    void this.router.navigate(['/profile']);
  }

  goToNotifications(): void {
    void this.router.navigate(['/notifications']);
  }

  toggleProfileMenu(): void {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  logout(): void {
    this.isProfileMenuOpen = false;
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  @HostListener('document:click', ['$event'])
  closeProfileMenuOutside(event: MouseEvent): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const target = event.target;

    if (
      target instanceof Node &&
      !this.elementRef.nativeElement.contains(target)
    ) {
      this.isProfileMenuOpen = false;
    }
  }
}
