import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }

  const returnUrl = state.url.startsWith('/app/') ? state.url : '/app/map';
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl },
  });
};
