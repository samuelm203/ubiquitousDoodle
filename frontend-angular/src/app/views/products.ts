import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, Product, ProductForm } from '../api';
import { Notify } from '../notify';
import { ModalDialog } from '../components/modal-dialog';
import { byName, money } from '../format';

@Component({
  selector: 'app-products',
  imports: [FormsModule, ModalDialog],
  template: `
    <div class="page-head">
      <h1>Produkte</h1>
      <div class="toolbar">
        <input [ngModel]="filter()" (ngModelChange)="filter.set($event)" type="search" placeholder="Suchen …" aria-label="Produkte suchen" />
        <button class="btn btn-primary" (click)="open()">+ Neues Produkt</button>
      </div>
    </div>
    <p class="muted note">Produkte anlegen und ändern darf nur ein Admin.</p>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr><th>SKU</th><th>Name</th><th class="num">Preis</th><th>Status</th></tr>
        </thead>
        <tbody>
          @for (p of visible(); track p.id) {
            <tr class="clickable" [class.inactive]="!p.active" (click)="open(p)">
              <td><code>{{ p.sku }}</code></td>
              <td><strong>{{ p.name }}</strong></td>
              <td class="num">{{ money(p.price, p.currency) }}</td>
              <td><span class="badge" [class]="'badge ' + (p.active ? 'badge-success' : 'badge-neutral')">{{ p.active ? 'Aktiv' : 'Inaktiv' }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="empty">Keine Produkte gefunden</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (editing(); as e) {
      <app-modal-dialog [title]="e.id ? 'Produkt bearbeiten' : 'Neues Produkt'" (closed)="editing.set(null)">
        <form class="form-grid" ngNativeValidate (ngSubmit)="save()">
          <label class="field">
            <span>SKU{{ e.id ? ' (nicht änderbar)' : '' }}</span>
            <input name="sku" [(ngModel)]="form.sku" [disabled]="!!e.id" required autofocus />
          </label>
          <label class="field"><span>Name</span><input name="name" [(ngModel)]="form.name" required /></label>
          <label class="field">
            <span>Preis (CHF)</span>
            <input name="price" [(ngModel)]="form.price" inputmode="decimal" pattern="\\d+([.,]\\d{1,2})?" required />
          </label>
          <label class="check"><input name="active" [(ngModel)]="form.active" type="checkbox" /> Aktiv</label>
          <div class="form-actions">
            <button type="button" class="btn" (click)="editing.set(null)">Abbrechen</button>
            <button class="btn btn-primary">Speichern</button>
          </div>
        </form>
      </app-modal-dialog>
    }
  `,
  styles: `
    .note { margin: -10px 0 16px; font-size: 0.9rem; }
  `,
})
export class Products implements OnInit {
  private readonly api = inject(Api);
  private readonly notify = inject(Notify);

  protected readonly money = money;
  protected readonly products = signal<Product[]>([]);
  protected readonly filter = signal('');
  protected readonly editing = signal<Partial<Product> | null>(null);
  protected form: ProductForm = { sku: '', name: '', price: '', active: true };

  protected readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const list = this.products().slice().sort(byName);
    if (!q) return list;
    return list.filter((p) => [p.name, p.sku].some((v) => v?.toLowerCase().includes(q)));
  });

  ngOnInit(): Promise<void> {
    return this.load();
  }

  private async load(): Promise<void> {
    this.products.set((await this.notify.attempt(this.api.products.list)) ?? []);
  }

  protected open(product?: Product): void {
    this.form = product
      ? { sku: product.sku, name: product.name, price: String(product.price), active: product.active }
      : { sku: '', name: '', price: '', active: true };
    this.editing.set(product ?? {});
  }

  protected async save(): Promise<void> {
    const id = this.editing()?.id;
    // Preis als String schicken, damit BigDecimal im Backend exakt bleibt (z. B. "12.50")
    const body: ProductForm = { ...this.form, price: String(this.form.price).replace(',', '.') };
    const ok = await this.notify.attempt(
      () => (id ? this.api.products.update(id, body) : this.api.products.create(body)),
      id ? 'Produkt gespeichert' : 'Produkt angelegt',
    );
    if (ok) {
      this.editing.set(null);
      await this.load();
    }
  }
}
