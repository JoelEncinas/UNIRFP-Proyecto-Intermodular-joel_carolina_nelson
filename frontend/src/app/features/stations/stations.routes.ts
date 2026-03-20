import { Routes } from '@angular/router';

export const STATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/stations-page/stations-page').then((m) => m.StationsPage),
  },
];
