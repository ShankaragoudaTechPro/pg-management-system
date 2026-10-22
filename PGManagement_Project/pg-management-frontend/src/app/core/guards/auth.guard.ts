
import { inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // Allow server-side rendering without accessing localStorage
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const token = localStorage.getItem('pg_token');

  if (token) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
