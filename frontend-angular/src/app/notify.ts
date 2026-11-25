import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  kind: 'success' | 'error';
}

@Injectable({ providedIn: 'root' })
export class Notify {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  notify(message: string, kind: Toast['kind'] = 'success'): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, message, kind }]);
    setTimeout(() => this.toasts.update((list) => list.filter((t) => t.id !== id)), kind === 'error' ? 6000 : 3000);
  }

  /** Führt einen API-Aufruf aus und zeigt Fehler als Toast. Gibt bei Fehler undefined zurück. */
  async attempt<T>(fn: () => Promise<T>, successMessage?: string): Promise<T | undefined> {
    try {
      const result = await fn();
      if (successMessage) this.notify(successMessage);
      return result;
    } catch (e) {
      const err = e as { status?: number; message?: string };
      if (err.status !== 401) this.notify(err.message ?? 'Unbekannter Fehler', 'error');
      return undefined;
    }
  }
}
