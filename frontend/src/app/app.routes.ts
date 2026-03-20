import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth-guard';
import { redirectIfAuthGuard } from './core/auth/redirect-if-auth-guard';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [redirectIfAuthGuard],
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/app-shell/app-shell').then((m) => m.AppShell),
    children: [
      {
        path: 'map',
        loadChildren: () =>
          import('./features/map/map.routes').then((m) => m.MAP_ROUTES),
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('./features/profile/profile.routes').then((m) => m.PROFILE_ROUTES),
      },
      {
        path: 'history',
        loadChildren: () =>
          import('./features/history/history.routes').then((m) => m.HISTORY_ROUTES),
      },
      {
        path: 'stations',
        loadChildren: () =>
          import('./features/stations/stations.routes').then((m) => m.STATIONS_ROUTES),
      },
      {
        path: 'terms',
        loadChildren: () =>
          import('./features/terms/terms.routes').then((m) => m.TERMS_ROUTES),
      },
      { path: '', pathMatch: 'full', redirectTo: 'map' },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'auth/login' },
  { path: '**', redirectTo: 'auth/login' },
];
