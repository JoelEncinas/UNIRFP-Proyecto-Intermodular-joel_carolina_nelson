import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { Auth } from '../auth/auth';
import { isProtectedApiRequest } from './protected-api-url';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isProtectedApiRequest(req.url)) {
    return next(req);
  }

  const auth = inject(Auth);
  const token = auth.getValidToken();
  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(authReq);
};

export { isProtectedApiRequest };
