import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { Auth } from '../auth/auth';
import { isProtectedApiRequest } from './protected-api-url';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const isProtected = isProtectedApiRequest(req.url);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        isProtected &&
        error instanceof HttpErrorResponse &&
        (error.status === 401 || error.status === 403)
      ) {
        auth.logout();
        const currentUrl = router.url || '/app/map';
        if (!currentUrl.startsWith('/auth/login')) {
          void router.navigate(['/auth/login'], {
            queryParams: { returnUrl: currentUrl },
          });
        }
      }

      return throwError(() => error);
    }),
  );
};
