import { Routes } from '@angular/router';

export const TERMS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/terms-page/terms-page').then((m) => m.TermsPage),
  },
];
