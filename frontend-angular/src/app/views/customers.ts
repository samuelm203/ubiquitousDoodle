import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, Customer, CustomerForm } from '../api';
import { Notify } from '../notify';
import { ModalDialog } from '../components/modal-dialog';
import { byName } from '../format';

@Component({
  selector: 'app-customers',
  imports: [FormsModule, ModalDialog],
  template: `
    <div class="page-head">
      <h1>Kunden</h1>
      <div class="toolbar">
        <input [ngModel]="filter()" (ngModelChange)="filter.set($event)" type="search" placeholder="Suchen …" aria-label="Kunden suchen" />
        <button class="btn btn-primary" (click)="open()">+ Neuer Kunde</button>
      </div>
    </div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr><th>Name</th><th>E-Mail</th><th>Ort</th><th>Status</th></tr>
        </thead>
        <tbody>
          @for (c of visible(); track c.id) {
            <tr class="clickable" [class.inactive]="!c.active" (click)="open(c)">
              <td><strong>{{ c.name }}</strong></td>
              <td>{{ c.email }}</td>
              <td>{{ c.city }}</td>
              <td><span class="badge" [class]="'badge ' + (c.active ? 'badge-success' : 'badge-neutral')">{{ c.active ? 'Aktiv' : 'Inaktiv' }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="empty">Keine Kunden gefunden</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (editing(); as e) {
      <app-modal-dialog [title]="e.id ? 'Kunde bearbeiten' : 'Neuer Kunde'" (closed)="editing.set(null)">
        <form class="form-grid" ngNativeValidate (ngSubmit)="save()">
          <label class="field"><span>Name</span><input name="name" [(ngModel)]="form.name" required autofocus /></label>
          <label class="field"><span>E-Mail</span><input name="email" [(ngModel)]="form.email" type="email" required /></label>
          <label class="field"><span>Ort</span><input name="city" [(ngModel)]="form.city" /></label>
          <label class="check"><input name="active" [(ngModel)]="form.active" type="checkbox" /> Aktiv</label>
          <div class="form-actions">
            @if (e.id) {
              <button type="button" class="btn btn-danger" style="margin-right: auto" (click)="remove()">Löschen</button>
            }
            <button type="button" class="btn" (click)="editing.set(null)">Abbrechen</button>
            <button class="btn btn-primary">Speichern</button>
          </div>
        </form>
      </app-modal-dialog>
    }
  `,
})
export class Customers implements OnInit {
  private readonly api = inject(Api);
  private readonly notify = inject(Notify);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly filter = signal('');
  /** null = geschlossen, {} = neu, {id,...} = bearbeiten */
  protected readonly editing = signal<Partial<Customer> | null>(null);
  protected form: CustomerForm = { name: '', email: '', city: '', active: true };

  protected readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const list = this.customers().slice().sort(byName);
    if (!q) return list;
    return list.filter((c) => [c.name, c.email, c.city].some((v) => v?.toLowerCase().includes(q)));
  });

  ngOnInit(): Promise<void> {
    return this.load();
  }

  private async load(): Promise<void> {
    this.customers.set((await this.notify.attempt(this.api.customers.list)) ?? []);
  }

  protected open(customer?: Customer): void {
    this.form = customer
      ? { name: customer.name, email: customer.email, city: customer.city ?? '', active: customer.active }
      : { name: '', email: '', city: '', active: true };
    this.editing.set(customer ?? {});
  }

  protected async save(): Promise<void> {
    const id = this.editing()?.id;
    const body: CustomerForm = { ...this.form, city: this.form.city || null };
    const ok = await this.notify.attempt(
      () => (id ? this.api.customers.update(id, body) : this.api.customers.create(body)),
      id ? 'Kunde gespeichert' : 'Kunde angelegt',
    );
    if (ok) {
      this.editing.set(null);
      await this.load();
    }
  }

  protected async remove(): Promise<void> {
    const c = this.editing();
    if (!c?.id) return;
    const id = c.id;
    if (!window.confirm(`Kunde «${c.name}» wirklich löschen?`)) return;
    const ok = await this.notify.attempt(() => this.api.customers.remove(id).then(() => true), 'Kunde gelöscht');
    if (ok) {
      this.editing.set(null);
      await this.load();
    }
  }
}
