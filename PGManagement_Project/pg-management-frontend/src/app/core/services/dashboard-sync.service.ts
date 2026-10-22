import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

/**
 * Broadcasts successful writes that affect dashboard aggregates.
 * The dashboard subscribes only while mounted, so no polling or timer is retained
 * when the user navigates to another feature.
 */
@Injectable({ providedIn: 'root' })
export class DashboardSyncService {
  private readonly changedSubject = new Subject<void>();
  readonly changes$: Observable<void> = this.changedSubject.asObservable();

  notifyDataChanged(): void {
    this.changedSubject.next();
  }
}
