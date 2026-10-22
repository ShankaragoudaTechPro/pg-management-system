
import { Routes } from '@angular/router';

import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { authGuard } from './core/guards/auth.guard';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { PgCreateComponent } from './features/pg/pg-create/pg-create';

import { MaintenanceLoginComponent } from './features/maintenance/maintenance-login';
import { MaintenanceRegisterComponent } from './features/maintenance/maintenance-register';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'maintenance/login',
    component: MaintenanceLoginComponent
  },
  {
    path: 'maintenance/register',
    component: MaintenanceRegisterComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password')
        .then(m => m.ForgotPasswordComponent)
  },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard/dashboard')
            .then(m => m.Dashboard)
      },
      {
        path: 'pgs/add',
        component: PgCreateComponent
      },
      {
        path: 'pgs',
        loadComponent: () =>
          import('./features/pg/pg-list/pg-list')
            .then(m => m.PgListComponent)
      },
      {
        path: 'rooms',
        loadComponent: () =>
          import('./features/rooms/rooms')
            .then(m => m.RoomsComponent)
      },
      {
        path: 'tenants',
        loadComponent: () =>
          import('./features/tenants/tenants')
            .then(m => m.TenantsComponent)
      },
      {
        path: 'rent',
        loadComponent: () =>
          import('./features/rent/rent')
            .then(m => m.RentComponent)
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications')
            .then(m => m.NotificationsComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile')
            .then(m => m.ProfileComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
