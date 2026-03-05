import { CanActivateFn } from '@angular/router';

export const redirectIfAuthGuard: CanActivateFn = (route, state) => {
  return true;
};
