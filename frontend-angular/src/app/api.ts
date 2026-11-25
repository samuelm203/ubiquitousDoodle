import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, firstValueFrom, throwError } from 'rxjs';
import { Auth } from './auth';
import { OrderStatus } from './format';

export class ApiError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

export interface Dashboard {
  revenue: number | string;
  currency: string;
  customers: number;
  products: number;
  ordersByStatus: Partial<Record<OrderStatus, number>>;
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  city: string | null;
  active: boolean;
}
export type CustomerForm = Omit<Customer, 'id'>;

export interface Product {
  id: string;
  sku: string;
  name: string;
  price: number | string;
  currency: string;
  active: boolean;
}
export interface ProductForm {
  sku: string;
  name: string;
  price: string;
  active: boolean;
}

export interface OrderLine {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number | string;
  lineTotal: number | string;
}

export interface Order {
  id: string;
  number: string;
  customerName: string;
  status: OrderStatus;
  createdAt: string | null;
  placedAt: string | null;
  total: number | string;
  currency: string;
  lines: OrderLine[];
}

/**
 * Hängt Auth- und Marker-Header an jede /api-Anfrage und übersetzt Fehler in ApiError.
 * Bei 401 wird abgemeldet und zum Login geleitet.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);

  let headers = req.headers
    .set('Accept', 'application/json')
    // Ohne diesen Header schickt Spring Security bei 401 "WWW-Authenticate: Basic",
    // und der Browser öffnet seinen eigenen Login-Dialog.
    .set('X-Requested-With', 'XMLHttpRequest');
  const token = auth.token();
  if (token) headers = headers.set('Authorization', `Basic ${token}`);

  return next(req.clone({ headers })).pipe(
    catchError((e: unknown) => {
      if (!(e instanceof HttpErrorResponse)) return throwError(() => e);
      if (e.status === 0) return throwError(() => new ApiError(0, 'Server nicht erreichbar'));
      if (e.status === 401) {
        auth.logout();
        if (!router.url.startsWith('/login')) {
          void router.navigate(['/login'], { queryParams: { redirect: router.url } });
        }
        return throwError(() => new ApiError(401, 'Benutzername oder Passwort falsch'));
      }
      if (e.status === 403) return throwError(() => new ApiError(403, 'Dafür fehlt die Berechtigung (nur Admin)'));
      // RFC-9457 Problem Details: fachliche Meldung steht in "detail"
      const body = e.error as { detail?: string; title?: string } | null;
      return throwError(() => new ApiError(e.status, body?.detail || body?.title || `Fehler ${e.status}`));
    }),
  );
};

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  private get = <T>(path: string) => firstValueFrom(this.http.get<T>(`/api${path}`));
  private post = <T>(path: string, body?: unknown) => firstValueFrom(this.http.post<T>(`/api${path}`, body ?? null));
  private put = <T>(path: string, body: unknown) => firstValueFrom(this.http.put<T>(`/api${path}`, body));
  private del = <T>(path: string) => firstValueFrom(this.http.delete<T>(`/api${path}`));

  dashboard = () => this.get<Dashboard>('/dashboard');

  customers = {
    list: () => this.get<Customer[]>('/customers'),
    create: (form: CustomerForm) => this.post<Customer>('/customers', form),
    update: (id: string, form: CustomerForm) => this.put<Customer>(`/customers/${id}`, form),
    remove: (id: string) => this.del<void>(`/customers/${id}`),
  };

  products = {
    list: () => this.get<Product[]>('/products'),
    create: (form: ProductForm) => this.post<Product>('/products', form),
    update: (id: string, form: ProductForm) => this.put<Product>(`/products/${id}`, form),
  };

  orders = {
    list: () => this.get<Order[]>('/orders'),
    get: (id: string) => this.get<Order>(`/orders/${id}`),
    create: (customerId: string) => this.post<Order>('/orders', { customerId }),
    addLine: (id: string, productId: string, quantity: number) =>
      this.post<Order>(`/orders/${id}/lines`, { productId, quantity }),
    removeLine: (id: string, productId: string) => this.del<Order>(`/orders/${id}/lines/${productId}`),
    place: (id: string) => this.post<Order>(`/orders/${id}/place`),
    ship: (id: string) => this.post<Order>(`/orders/${id}/ship`),
    cancel: (id: string) => this.post<Order>(`/orders/${id}/cancel`),
  };
}
