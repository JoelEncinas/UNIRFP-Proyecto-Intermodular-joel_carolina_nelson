import { Routes } from '@angular/router';

export const HISTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/history-page/history-page').then((m) => m.HistoryPage),
  },
];
