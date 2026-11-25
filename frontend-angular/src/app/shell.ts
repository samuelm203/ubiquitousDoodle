import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from './auth';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">Orders</div>
        <nav>
          @for (item of nav; track item.to) {
            <a
              class="nav-link"
              [routerLink]="item.to"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.to === '/' }"
            >
              <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
            </a>
          }
        </nav>
        <div class="sidebar-foot">
          <span class="muted">Angemeldet als <strong>{{ auth.username() }}</strong></span>
          <button class="btn btn-ghost" (click)="signOut()">Abmelden</button>
        </div>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
})
export class Shell {
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly nav = [
    { to: '/', label: 'Übersicht', icon: '◧' },
    { to: '/orders', label: 'Bestellungen', icon: '▤' },
    { to: '/customers', label: 'Kunden', icon: '◎' },
    { to: '/products', label: 'Produkte', icon: '▣' },
  ];

  protected signOut(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
