import { Component, OnInit, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Api, Customer, Order, Product } from '../api';
import { Notify } from '../notify';
import { StatusBadge } from '../components/status-badge';
import { STATUS_LIST, byName, dateTime, money, newestFirst } from '../format';

@Component({
  selector: 'app-orders',
  imports: [FormsModule, StatusBadge],
  template: `
    <div class="page-head">
      <h1>Bestellungen</h1>
      <form class="toolbar" (ngSubmit)="createOrder()">
        <select name="newCustomer" [(ngModel)]="newCustomer" aria-label="Kunde für neue Bestellung">
          <option value="">Kunde wählen …</option>
          @for (c of activeCustomers(); track c.id) {
            <option [value]="c.id">{{ c.name }}</option>
          }
        </select>
        <button class="btn btn-primary" [disabled]="busy()">+ Neue Bestellung</button>
      </form>
    </div>

    <div class="layout" [class.with-detail]="current()">
      <section class="card">
        <div class="filters">
          <button class="chip" [class.on]="!statusFilter()" (click)="statusFilter.set('')">Alle</button>
          @for (s of statuses; track s.key) {
            <button class="chip" [class.on]="statusFilter() === s.key" (click)="statusFilter.set(s.key)">{{ s.label }}</button>
          }
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Nr.</th><th>Kunde</th><th>Status</th><th>Erfasst</th><th class="num">Total</th></tr>
            </thead>
            <tbody>
              @for (o of visible(); track o.id) {
                <tr class="clickable" [class.selected]="o.id === id()" (click)="select(o)">
                  <td><code>{{ o.number }}</code></td>
                  <td>{{ o.customerName }}</td>
                  <td><app-status-badge [status]="o.status" /></td>
                  <td class="muted">{{ dateTime(o.createdAt) }}</td>
                  <td class="num">{{ money(o.total, o.currency) }}</td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="empty">Keine Bestellungen</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      @if (current(); as cur) {
        <section class="card card-pad detail">
          <header class="detail-head">
            <div>
              <h2>Bestellung {{ cur.number }}</h2>
              <p class="muted">{{ cur.customerName }}</p>
            </div>
            <button class="btn btn-ghost btn-icon" aria-label="Detail schliessen" (click)="select(null)">✕</button>
          </header>

          <dl class="meta">
            <div><dt>Status</dt><dd><app-status-badge [status]="cur.status" /></dd></div>
            <div><dt>Erfasst</dt><dd>{{ dateTime(cur.createdAt) }}</dd></div>
            @if (cur.placedAt) {
              <div><dt>Aufgegeben</dt><dd>{{ dateTime(cur.placedAt) }}</dd></div>
            }
          </dl>

          <table class="lines">
            <thead>
              <tr>
                <th>Produkt</th><th class="num">Menge</th><th class="num">Preis</th><th class="num">Total</th>
                @if (isDraft()) { <th></th> }
              </tr>
            </thead>
            <tbody>
              @for (l of cur.lines; track l.productId) {
                <tr>
                  <td>{{ l.productName }}</td>
                  <td class="num">{{ l.quantity }}</td>
                  <td class="num">{{ money(l.unitPrice, cur.currency) }}</td>
                  <td class="num">{{ money(l.lineTotal, cur.currency) }}</td>
                  @if (isDraft()) {
                    <td class="num">
                      <button class="btn btn-ghost btn-icon btn-danger" [disabled]="busy()" aria-label="Position entfernen" (click)="removeLine(l.productId)">🗑</button>
                    </td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="isDraft() ? 5 : 4" class="empty">Noch keine Positionen</td></tr>
              }
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3"><strong>Total</strong></td>
                <td class="num"><strong>{{ money(cur.total, cur.currency) }}</strong></td>
                @if (isDraft()) { <td></td> }
              </tr>
            </tfoot>
          </table>

          @if (isDraft()) {
            <form class="add-line" (ngSubmit)="addLine()">
              <select name="lineProduct" [(ngModel)]="lineProduct" aria-label="Produkt">
                <option value="">Produkt wählen …</option>
                @for (p of activeProducts(); track p.id) {
                  <option [value]="p.id">{{ p.name }} ({{ money(p.price, p.currency) }})</option>
                }
              </select>
              <input name="lineQty" [(ngModel)]="lineQty" type="number" min="1" step="1" aria-label="Menge" class="qty" />
              <button class="btn" [disabled]="busy()">Hinzufügen</button>
            </form>
          }

          @if (isDraft() || isPlaced()) {
            <div class="actions">
              @if (isDraft()) {
                <button class="btn btn-primary" [disabled]="busy()" (click)="place()">✓ Aufgeben</button>
              }
              @if (isPlaced()) {
                <button class="btn btn-primary" [disabled]="busy()" (click)="ship()">Versenden</button>
              }
              <button class="btn btn-danger" [disabled]="busy()" (click)="cancel()">Stornieren</button>
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .layout { display: grid; gap: 16px; grid-template-columns: 1fr; align-items: start; }
    .layout.with-detail { grid-template-columns: minmax(0, 1fr) minmax(360px, 520px); }
    @media (max-width: 1100px) { .layout.with-detail { grid-template-columns: 1fr; } }
    .filters { display: flex; gap: 6px; padding: 12px 14px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
    .chip { font: inherit; font-size: 0.85rem; border: 1px solid var(--border); background: transparent; color: var(--muted); border-radius: 999px; padding: 3px 12px; cursor: pointer; }
    .chip.on { background: var(--primary); border-color: var(--primary); color: #fff; }
    .detail { display: grid; gap: 16px; position: sticky; top: 20px; }
    .detail-head { display: flex; justify-content: space-between; align-items: flex-start; }
    .detail-head p { margin: 4px 0 0; }
    .meta { display: flex; gap: 28px; margin: 0; flex-wrap: wrap; }
    .meta dt { font-size: 0.8rem; color: var(--muted); margin-bottom: 4px; }
    .meta dd { margin: 0; }
    .lines th, .lines td { padding: 8px 6px; }
    .lines tfoot td { border-top: 2px solid var(--border); border-bottom: none; }
    .add-line { display: flex; gap: 8px; }
    .add-line select { flex: 1; }
    .qty { width: 80px; }
    .actions { display: flex; gap: 8px; border-top: 1px solid var(--border); padding-top: 16px; }
  `,
})
export class Orders implements OnInit {
  private readonly api = inject(Api);
  private readonly notify = inject(Notify);
  private readonly router = inject(Router);

  /** Route-Parameter /orders/:id (per withComponentInputBinding) */
  readonly id = input<string>();

  protected readonly money = money;
  protected readonly dateTime = dateTime;
  protected readonly statuses = STATUS_LIST;

  protected readonly orders = signal<Order[]>([]);
  private readonly customers = signal<Customer[]>([]);
  private readonly products = signal<Product[]>([]);
  protected readonly statusFilter = signal('');
  protected readonly busy = signal(false);

  protected newCustomer = '';
  protected lineProduct = '';
  protected lineQty = 1;

  protected readonly current = computed(() => this.orders().find((o) => o.id === this.id()) ?? null);
  protected readonly isDraft = computed(() => this.current()?.status === 'DRAFT');
  protected readonly isPlaced = computed(() => this.current()?.status === 'PLACED');
  protected readonly activeCustomers = computed(() => this.customers().filter((c) => c.active).sort(byName));
  protected readonly activeProducts = computed(() => this.products().filter((p) => p.active).sort(byName));

  protected readonly visible = computed(() =>
    this.orders()
      .filter((o) => !this.statusFilter() || o.status === this.statusFilter())
      .slice()
      .sort(newestFirst),
  );

  constructor() {
    // Beim Wechsel der Auswahl die Positions-Eingabe zurücksetzen
    effect(() => {
      this.id();
      this.lineProduct = '';
      this.lineQty = 1;
    });
  }

  async ngOnInit(): Promise<void> {
    const [o, c, p] = await Promise.all([
      this.notify.attempt(this.api.orders.list),
      this.notify.attempt(this.api.customers.list),
      this.notify.attempt(this.api.products.list),
    ]);
    this.orders.set(o ?? []);
    this.customers.set(c ?? []);
    this.products.set(p ?? []);
  }

  protected select(o: Order | null): void {
    const target = o && o.id !== this.id() ? ['/orders', o.id] : ['/orders'];
    void this.router.navigate(target, { replaceUrl: true });
  }

  /** Ersetzt die Bestellung in der Liste durch die Antwort des Servers. */
  private apply(updated: Order): void {
    this.orders.update((list) => {
      const i = list.findIndex((o) => o.id === updated.id);
      return i >= 0 ? list.map((o, j) => (j === i ? updated : o)) : [...list, updated];
    });
  }

  private async run(fn: () => Promise<Order>, message?: string): Promise<Order | undefined> {
    this.busy.set(true);
    const updated = await this.notify.attempt(fn, message);
    this.busy.set(false);
    if (updated) this.apply(updated);
    return updated;
  }

  protected async createOrder(): Promise<void> {
    if (!this.newCustomer) {
      this.notify.notify('Bitte zuerst einen Kunden wählen', 'error');
      return;
    }
    const customerId = this.newCustomer;
    const created = await this.run(() => this.api.orders.create(customerId), 'Bestellung angelegt');
    if (created) {
      this.newCustomer = '';
      this.statusFilter.set('');
      await this.router.navigate(['/orders', created.id], { replaceUrl: true });
    }
  }

  protected async addLine(): Promise<void> {
    const cur = this.current();
    const qty = Number(this.lineQty);
    if (!cur || !this.lineProduct || !Number.isInteger(qty) || qty < 1) {
      this.notify.notify('Produkt und Menge (≥ 1) angeben', 'error');
      return;
    }
    const productId = this.lineProduct;
    const ok = await this.run(() => this.api.orders.addLine(cur.id, productId, qty));
    if (ok) {
      this.lineProduct = '';
      this.lineQty = 1;
    }
  }

  protected removeLine(productId: string): void {
    const cur = this.current();
    if (cur) void this.run(() => this.api.orders.removeLine(cur.id, productId));
  }

  protected place(): void {
    const cur = this.current();
    if (cur) void this.run(() => this.api.orders.place(cur.id), 'Bestellung aufgegeben');
  }

  protected ship(): void {
    const cur = this.current();
    if (cur) void this.run(() => this.api.orders.ship(cur.id), 'Bestellung versendet');
  }

  protected cancel(): void {
    const cur = this.current();
    if (cur && window.confirm(`Bestellung ${cur.number} stornieren?`)) {
      void this.run(() => this.api.orders.cancel(cur.id), 'Bestellung storniert');
    }
  }
}
