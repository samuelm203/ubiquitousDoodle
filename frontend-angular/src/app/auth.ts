import { Injectable, computed, signal } from '@angular/core';

// HTTP Basic: Zugangsdaten nur für die Browser-Sitzung merken (sessionStorage, nicht localStorage).
const KEY = 'orders.auth';

interface Saved {
  username: string;
  token: string;
  admin?: boolean;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly saved: Saved | null = JSON.parse(sessionStorage.getItem(KEY) ?? 'null');

  readonly username = signal<string | null>(this.saved?.username ?? null);
  readonly token = signal<string | null>(this.saved?.token ?? null);
  /** Es gibt keinen Rollen-Endpoint: beim Login wird geprüft, ob /api/properties erreichbar ist (nur ADMIN). */
  readonly admin = signal(this.saved?.admin ?? false);
  readonly loggedIn = computed(() => this.token() !== null);

  setCredentials(username: string, password: string): void {
    // btoa kann nur Latin-1; über TextEncoder sind auch Umlaute im Passwort möglich
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    const token = btoa(String.fromCharCode(...bytes));
    this.username.set(username);
    this.token.set(token);
    this.admin.set(false);
    sessionStorage.setItem(KEY, JSON.stringify({ username, token }));
  }

  setAdmin(admin: boolean): void {
    this.admin.set(admin);
    sessionStorage.setItem(KEY, JSON.stringify({ username: this.username(), token: this.token(), admin }));
  }

  logout(): void {
    this.username.set(null);
    this.token.set(null);
    this.admin.set(false);
    sessionStorage.removeItem(KEY);
  }
}
