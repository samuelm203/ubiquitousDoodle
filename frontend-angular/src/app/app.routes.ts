import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes, UrlMatchResult, UrlSegment } from '@angular/router';
import { Auth } from './auth';

const requireLogin: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (auth.loggedIn()) return true;
  return router.createUrlTree(['/login'], {
    queryParams: state.url !== '/' ? { redirect: state.url } : {},
  });
};

const redirectIfLoggedIn: CanActivateFn = () => {
  const auth = inject(Auth);
  return auth.loggedIn() ? inject(Router).createUrlTree(['/']) : true;
};

/** Entspricht "/orders/:id?" – eine einzige Route, damit die Komponente beim Wechsel der Auswahl erhalten bleibt. */
function ordersMatcher(segments: UrlSegment[]): UrlMatchResult | null {
  if (segments.length === 0 || segments.length > 2 || segments[0].path !== 'orders') return null;
  return segments.length === 2 ? { consumed: segments, posParams: { id: segments[1] } } : { consumed: segments };
}

export const routes: Routes = [
  {
    path: 'login',
    title: '',
    canActivate: [redirectIfLoggedIn],
    loadComponent: () => import('./views/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [requireLogin],
    loadComponent: () => import('./shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', title: 'Übersicht', loadComponent: () => import('./views/dashboard').then((m) => m.Dashboard) },
      { matcher: ordersMatcher, title: 'Bestellungen', loadComponent: () => import('./views/orders').then((m) => m.Orders) },
      { path: 'customers', title: 'Kunden', loadComponent: () => import('./views/customers').then((m) => m.Customers) },
      { path: 'products', title: 'Produkte', loadComponent: () => import('./views/products').then((m) => m.Products) },
    ],
  },
  { path: '**', redirectTo: '' },
];
