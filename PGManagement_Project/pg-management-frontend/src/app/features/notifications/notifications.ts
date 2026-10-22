import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  finalize,
  timeout,
  TimeoutError
} from 'rxjs';

import { NotificationService } from './notification.service';
import {
  AppNotification,
  NotificationType
} from './notification.model';

type NotificationFilter = 'ALL' | 'UNREAD' | 'READ';
type SelectedNotificationType = 'ALL' | NotificationType;

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss'
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private readonly notificationService = inject(NotificationService);

  notifications: AppNotification[] = [];

  loading = false;
  refreshing = false;
  updatingAll = false;

  readonly updatingIds = new Set<number>();
  readonly deletingIds = new Set<number>();

  errorMessage = '';
  successMessage = '';

  searchText = '';
  selectedFilter: NotificationFilter = 'ALL';
  selectedType: SelectedNotificationType = 'ALL';

  private messageTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.loadNotifications();
  }

  ngOnDestroy(): void {
    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
      this.messageTimer = undefined;
    }
  }

  loadNotifications(): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.notificationService.getAll()
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.loading = false;
          this.refreshing = false;
        })
      )
      .subscribe({
        next: (data: AppNotification[]) => {
          this.notifications = Array.isArray(data) ? data : [];
        },
        error: (error: unknown) => {
          console.error('Failed to load notifications:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to load notifications. Please try again.'
          );
        }
      });
  }

  refreshNotifications(): void {
    if (this.loading) {
      return;
    }

    this.refreshing = true;
    this.loadNotifications();
  }

  get filteredNotifications(): AppNotification[] {
    const search = this.searchText.trim().toLowerCase();

    return this.notifications
      .filter(notification => {
        const matchesSearch =
          !search ||
          notification.title?.toLowerCase().includes(search) ||
          notification.message?.toLowerCase().includes(search) ||
          notification.tenantName?.toLowerCase().includes(search) ||
          notification.roomNumber?.toLowerCase().includes(search);

        const matchesRead =
          this.selectedFilter === 'ALL' ||
          (this.selectedFilter === 'UNREAD' && !notification.read) ||
          (this.selectedFilter === 'READ' && notification.read);

        const matchesType =
          this.selectedType === 'ALL' ||
          notification.type === this.selectedType;

        return matchesSearch && matchesRead && matchesType;
      })
      .sort((a, b) => {
        const dateA = Date.parse(a.createdAt);
        const dateB = Date.parse(b.createdAt);

        return (
          (Number.isFinite(dateB) ? dateB : 0) -
          (Number.isFinite(dateA) ? dateA : 0)
        );
      });
  }

  get unreadCount(): number {
    return this.notifications.filter(item => !item.read).length;
  }

  get rentReminderCount(): number {
    return this.notifications.filter(
      item => item.type === 'RENT' && !item.read
    ).length;
  }

  get systemAlertCount(): number {
    return this.notifications.filter(
      item => item.type === 'SYSTEM' && !item.read
    ).length;
  }

  get readCount(): number {
    return this.notifications.filter(item => item.read).length;
  }

  setFilter(filter: NotificationFilter): void {
    this.selectedFilter = filter;
  }

  markAsRead(notification: AppNotification): void {
    if (
      notification.read ||
      this.updatingIds.has(notification.id)
    ) {
      return;
    }

    this.errorMessage = '';
    this.updatingIds.add(notification.id);

    this.notificationService
      .markAsRead(notification.id)
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.updatingIds.delete(notification.id);
        })
      )
      .subscribe({
        next: () => {
          notification.read = true;
          this.showSuccess('Notification marked as read.');
        },
        error: (error: unknown) => {
          console.error('Failed to mark notification as read:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Could not update this notification.'
          );
        }
      });
  }

  markAllAsRead(): void {
    if (this.unreadCount === 0 || this.updatingAll) {
      return;
    }

    this.updatingAll = true;
    this.errorMessage = '';

    this.notificationService
      .markAllAsRead()
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.updatingAll = false;
        })
      )
      .subscribe({
        next: () => {
          this.notifications = this.notifications.map(item => ({
            ...item,
            read: true
          }));

          this.showSuccess('All notifications marked as read.');
        },
        error: (error: unknown) => {
          console.error('Failed to mark all notifications as read:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Could not mark all notifications as read.'
          );
        }
      });
  }

  deleteNotification(notification: AppNotification): void {
    if (
      this.deletingIds.has(notification.id) ||
      !confirm(`Delete "${notification.title}"?`)
    ) {
      return;
    }

    this.errorMessage = '';
    this.deletingIds.add(notification.id);

    this.notificationService
      .deleteNotification(notification.id)
      .pipe(
        timeout({ first: 10000 }),
        finalize(() => {
          this.deletingIds.delete(notification.id);
        })
      )
      .subscribe({
        next: () => {
          this.notifications = this.notifications.filter(
            item => item.id !== notification.id
          );

          this.showSuccess('Notification deleted.');
        },
        error: (error: unknown) => {
          console.error('Failed to delete notification:', error);

          this.errorMessage = this.getErrorMessage(
            error,
            'Could not delete this notification.'
          );
        }
      });
  }

  iconFor(type: NotificationType): string {
    const icons: Record<NotificationType, string> = {
      RENT: '₹',
      PAYMENT: '✓',
      TENANT: '♙',
      SYSTEM: '⚙',
      GENERAL: '●'
    };

    return icons[type] ?? '●';
  }

  labelFor(type: NotificationType): string {
    const labels: Record<NotificationType, string> = {
      RENT: 'Rent reminder',
      PAYMENT: 'Payment',
      TENANT: 'Tenant update',
      SYSTEM: 'System alert',
      GENERAL: 'General'
    };

    return labels[type] ?? 'Notification';
  }

  trackById(_: number, item: AppNotification): number {
    return item.id;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private showSuccess(message: string): void {
    this.successMessage = message;

    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
    }

    this.messageTimer = setTimeout(() => {
      this.successMessage = '';
      this.messageTimer = undefined;
    }, 3000);
  }

  private getErrorMessage(
    error: unknown,
    fallback: string
  ): string {
    if (error instanceof TimeoutError) {
      return 'Request timed out after 10 seconds. Please try again.';
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

      if (error.status === 404) {
        return 'The requested notification was not found.';
      }

      const backendMessage = error.error?.message;

      if (
        typeof backendMessage === 'string' &&
        backendMessage.trim().length > 0
      ) {
        return backendMessage;
      }
    }

    return fallback;
  }
}