import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, Dashboard as DashboardData, Order } from '../api';
import { Notify } from '../notify';
import { StatusBadge } from '../components/status-badge';
import { OrderStatus, STATUS_LIST, money, newestFirst } from '../format';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, StatusBadge],
  template: `
    <div class="page-head">
      <h1>Übersicht</h1>
    </div>
    <p class="muted">Framework: Angular</p>

    @if (data(); as d) {
      <div class="tiles">
        <div class="card card-pad tile tile-wide">
          <span class="muted">Umsatz (aufgegeben + versendet)</span>
          <strong>{{ money(d.revenue, d.currency) }}</strong>
        </div>
        <div class="card card-pad tile">
          <span class="muted">Offene Bestellungen</span>
          <strong>{{ byStatus('PLACED') }}</strong>
        </div>
        <div class="card card-pad tile">
          <span class="muted">Kunden</span>
          <strong>{{ d.customers }}</strong>
        </div>
        <div class="card card-pad tile">
          <span class="muted">Produkte</span>
          <strong>{{ d.products }}</strong>
        </div>
      </div>

      <div class="grid2">
        <section class="card card-pad">
          <h2>Bestellungen nach Status</h2>
          <ul class="bars">
            @for (s of statuses; track s.key) {
              <li>
                <app-status-badge [status]="s.key" />
                <div class="bar">
                  <div class="bar-fill" [class]="'bar-fill fill-' + s.tone" [style.width]="percent(s.key)"></div>
                </div>
                <span class="num">{{ byStatus(s.key) }}</span>
              </li>
            }
          </ul>
        </section>

        <section class="card">
          <div class="card-pad section-head">
            <h2>Neueste Bestellungen</h2>
            <a routerLink="/orders" class="muted">Alle anzeigen →</a>
          </div>
          <table>
            <tbody>
              @for (o of recent(); track o.id) {
                <tr class="clickable" (click)="open(o)">
                  <td><strong>{{ o.number }}</strong><br /><span class="muted">{{ o.customerName }}</span></td>
                  <td><app-status-badge [status]="o.status" /></td>
                  <td class="num">{{ money(o.total, o.currency) }}</td>
                </tr>
              } @empty {
                <tr><td class="empty" colspan="3">Noch keine Bestellungen</td></tr>
              }
            </tbody>
          </table>
        </section>
      </div>
    } @else {
      <p class="muted">Lade …</p>
    }
  `,
  styles: `
    .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 18px; }
    .tile { display: flex; flex-direction: column; gap: 6px; }
    .tile span { font-size: 0.9rem; }
    .tile strong { font-size: 1.8rem; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }
    .tile-wide { grid-column: span 2; }
    @media (max-width: 760px) { .tile-wide { grid-column: auto; } }
    .grid2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 14px; }
    .section-head { display: flex; justify-content: space-between; align-items: center; padding-bottom: 6px; }
    .section-head a { font-size: 0.9rem; text-decoration: none; }
    .bars { list-style: none; padding: 0; margin: 16px 0 0; display: grid; gap: 12px; }
    .bars li { display: grid; grid-template-columns: 100px 1fr 32px; align-items: center; gap: 12px; }
    .bar { height: 8px; border-radius: 4px; background: var(--bg); overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
    .fill-neutral { background: var(--muted); }
    .fill-info { background: var(--info); }
    .fill-success { background: var(--success); }
    .fill-danger { background: var(--danger); }
  `,
})
export class Dashboard implements OnInit {
  private readonly api = inject(Api);
  private readonly notify = inject(Notify);
  private readonly router = inject(Router);

  protected readonly money = money;
  protected readonly statuses = STATUS_LIST;
  protected readonly data = signal<DashboardData | null>(null);
  protected readonly recent = signal<Order[]>([]);

  protected readonly totalOrders = computed(() => this.statuses.reduce((sum, s) => sum + this.byStatus(s.key), 0));

  async ngOnInit(): Promise<void> {
    const [d, orders] = await Promise.all([
      this.notify.attempt(this.api.dashboard),
      this.notify.attempt(this.api.orders.list),
    ]);
    this.recent.set((orders ?? []).slice().sort(newestFirst).slice(0, 5));
    // Erst zuletzt setzen: "Lade …" verschwindet, wenn alles da ist
    this.data.set(d ?? null);
  }

  protected byStatus(s: OrderStatus): number {
    return this.data()?.ordersByStatus?.[s] ?? 0;
  }

  protected percent(s: OrderStatus): string {
    const total = this.totalOrders();
    return total ? `${(this.byStatus(s) / total) * 100}%` : '0';
  }

  protected open(o: Order): void {
    void this.router.navigate(['/orders', o.id]);
  }
}
