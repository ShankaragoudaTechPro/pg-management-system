import { inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);

  // Avoid accessing localStorage during server-side rendering.
  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }

  const isPublicAuthRequest =
    req.url.includes('/api/v1/auth/login') ||
    req.url.includes('/api/v1/auth/register') ||
    req.url.includes('/api/v1/auth/forgot-password') ||
    req.url.includes('/api/v1/auth/verify-otp');

  // Do not attach an existing token to public authentication requests.
  if (isPublicAuthRequest) {
    return next(req);
  }

  const token = localStorage.getItem('pg_token')?.trim();

  // Continue normally when no token is stored.
  if (!token) {
    return next(req);
  }

  // Attach the JWT to protected API requests.
  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};