import { Injectable, computed, signal } from '@angular/core';

// HTTP Basic: Zugangsdaten nur für die Browser-Sitzung merken (sessionStorage, nicht localStorage).
const KEY = 'orders.auth';

interface Saved {
  username: string;
  token: string;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly saved: Saved | null = JSON.parse(sessionStorage.getItem(KEY) ?? 'null');

  readonly username = signal<string | null>(this.saved?.username ?? null);
  readonly token = signal<string | null>(this.saved?.token ?? null);
  readonly loggedIn = computed(() => this.token() !== null);

  setCredentials(username: string, password: string): void {
    // btoa kann nur Latin-1; über TextEncoder sind auch Umlaute im Passwort möglich
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    const token = btoa(String.fromCharCode(...bytes));
    this.username.set(username);
    this.token.set(token);
    sessionStorage.setItem(KEY, JSON.stringify({ username, token }));
  }

  logout(): void {
    this.username.set(null);
    this.token.set(null);
    sessionStorage.removeItem(KEY);
  }
}
