import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Api } from '../api';
import { Auth } from '../auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <form class="card login-card" ngNativeValidate (ngSubmit)="submit()">
        <h1>Orders</h1>
        <p class="muted">Bitte anmelden</p>
        <label class="field">
          <span>Benutzername</span>
          <input name="username" [(ngModel)]="username" autocomplete="username" required autofocus />
        </label>
        <label class="field">
          <span>Passwort</span>
          <input name="password" [(ngModel)]="password" type="password" autocomplete="current-password" required />
        </label>
        @if (error()) {
          <p class="login-error" role="alert">{{ error() }}</p>
        }
        <button class="btn btn-primary login-btn" [disabled]="busy()">{{ busy() ? 'Anmelden …' : 'Anmelden' }}</button>
        <p class="muted hint">Demo: <code>admin / admin</code> oder <code>user / user</code></p>
      </form>
    </div>
  `,
  styles: `
    .login-page { min-height: 100vh; display: grid; place-items: center; padding: 16px; }
    .login-card { width: min(360px, 100%); padding: 28px; display: grid; gap: 14px; }
    .login-card p { margin: 0; }
    .login-btn { justify-content: center; padding: 9px; }
    .login-error { color: var(--danger); font-size: 0.9rem; }
    .hint { font-size: 0.85rem; text-align: center; }
  `,
})
export class Login {
  private readonly api = inject(Api);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  /** Query-Parameter ?redirect=… (per withComponentInputBinding) */
  readonly redirect = input<string>();

  protected username = '';
  protected password = '';
  protected readonly error = signal('');
  protected readonly busy = signal(false);

  protected async submit(): Promise<void> {
    this.error.set('');
    this.busy.set(true);
    this.auth.setCredentials(this.username.trim(), this.password);
    try {
      // HTTP Basic hat kein Login-Endpoint: ein geschützter GET prüft die Zugangsdaten
      await this.api.dashboard();
      // 403 = kein Admin (Firmenkonfiguration bleibt ausgeblendet); andere Fehler gelten ebenfalls als "nicht Admin"
      this.auth.setAdmin(await this.api.properties.companies().then(() => true, () => false));
      await this.router.navigateByUrl(this.redirect() || '/', { replaceUrl: true });
    } catch (e) {
      this.auth.logout();
      this.error.set((e as Error).message);
    } finally {
      this.busy.set(false);
    }
  }
}
