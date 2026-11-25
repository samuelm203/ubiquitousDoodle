import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, Company, PropertyChange, PropertyTable } from '../api';
import { Notify } from '../notify';

const SIZE = 100;
const SEP = '\u0000';

@Component({
  selector: 'app-properties',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-head">
      <h1>Firmenkonfiguration</h1>
      <div class="toolbar">
        <select [ngModel]="company()" (ngModelChange)="selectCompany($event)" aria-label="Firma">
          @for (c of companies(); track c.code) {
            <option [value]="c.code">{{ c.code }} ({{ c.keyCount }})</option>
          }
        </select>
        <input [ngModel]="filter()" (ngModelChange)="setFilter($event)" type="search" placeholder="Schlüssel filtern …" aria-label="Filter" />
        <label class="check"><input type="checkbox" [ngModel]="onlyDiff()" (ngModelChange)="setOnlyDiff($event)" /> Nur Unterschiede</label>
      </div>
    </div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>Schlüssel</th>
            @for (c of table()?.columns ?? []; track c) { <th>{{ c }}</th> }
          </tr>
        </thead>
        <tbody>
          @for (r of table()?.rows ?? []; track r.key) {
            <tr>
              <td class="key"><code>{{ r.key }}</code></td>
              @for (col of table()!.columns; track col; let i = $index) {
                <td class="cell" [class.changed]="edits().has(col + SEP + r.key)" [class.deleted]="edits().get(col + SEP + r.key) === null">
                  <input
                    [value]="shown(col, r.key, r.values[i])"
                    [placeholder]="shown(col, r.key, r.values[i]) === null || edits().get(col + SEP + r.key) === null ? 'fehlt' : ''"
                    [attr.aria-label]="col + ' ' + r.key"
                    (change)="edit(col, r.key, r.values[i], $any($event.target).value)"
                  />
                  @if (shown(col, r.key, r.values[i]) !== null) {
                    <button class="btn btn-ghost btn-icon x" title="Schlüssel löschen" (click)="remove(col, r.key, r.values[i])">×</button>
                  } @else if (edits().has(col + SEP + r.key)) {
                    <button class="btn btn-ghost btn-icon x" title="Rückgängig" (click)="undo(col, r.key)">↺</button>
                  }
                </td>
              }
            </tr>
          } @empty {
            <tr><td [attr.colspan]="(table()?.columns?.length ?? 0) + 1" class="empty">{{ loading() ? 'Lädt …' : 'Keine Schlüssel gefunden' }}</td></tr>
          }
        </tbody>
      </table>
    </div>

    <div class="foot">
      <span class="muted">{{ range() }}</span>
      <button class="btn" [disabled]="page() === 0" (click)="goto(page() - 1)">‹</button>
      <button class="btn" [disabled]="(page() + 1) * size >= (table()?.total ?? 0)" (click)="goto(page() + 1)">›</button>
      <span class="spacer"></span>
      @if (edits().size) {
        <span>{{ edits().size }} ungespeicherte Änderung(en)</span>
        <button class="btn" (click)="discard()">Verwerfen</button>
      }
      <button class="btn btn-primary" [disabled]="!edits().size || saving()" (click)="save()">Speichern</button>
    </div>
  `,
  styles: `
    .key { max-width: 380px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    td.cell { padding: 3px 6px; position: relative; min-width: 160px; }
    td.cell input { width: 100%; box-sizing: border-box; font-family: monospace; font-size: 0.85rem; padding: 4px 24px 4px 6px; }
    td.cell.changed input { background: color-mix(in srgb, var(--primary) 12%, transparent); }
    td.cell.deleted input { text-decoration: line-through; }
    .x { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); padding: 0 4px; }
    .foot { display: flex; gap: 8px; align-items: center; margin-top: 12px; flex-wrap: wrap; }
    .spacer { flex: 1; }
  `,
})
export class Properties implements OnInit {
  private readonly api = inject(Api);
  private readonly notify = inject(Notify);

  protected readonly SEP = SEP;
  protected readonly size = SIZE;
  protected readonly companies = signal<Company[]>([]);
  protected readonly company = signal('');
  protected readonly filter = signal('');
  protected readonly onlyDiff = signal(false);
  protected readonly page = signal(0);
  protected readonly table = signal<PropertyTable | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  /** Ungespeicherte Änderungen über alle Seiten: "spalte\0schlüssel" -> neuer Wert (null = löschen). */
  protected readonly edits = signal<ReadonlyMap<string, string | null>>(new Map());

  protected readonly range = computed(() => {
    const t = this.table();
    if (!t || !t.total) return '0 Zeilen';
    return `${t.page * t.size + 1}–${Math.min((t.page + 1) * t.size, t.total)} von ${t.total}`;
  });

  private timer: ReturnType<typeof setTimeout> | undefined;
  private requestId = 0;

  async ngOnInit(): Promise<void> {
    const list = (await this.notify.attempt(this.api.properties.companies)) ?? [];
    this.companies.set(list);
    if (list.length) await this.selectCompany(list[0].code);
  }

  protected async selectCompany(code: string): Promise<void> {
    if (this.edits().size && !window.confirm('Ungespeicherte Änderungen verwerfen?')) {
      this.company.set(this.company()); // Auswahl zurücksetzen
      return;
    }
    this.edits.set(new Map());
    this.company.set(code);
    this.page.set(0);
    await this.load();
  }

  protected setFilter(v: string): void {
    this.filter.set(v);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.goto(0), 250);
  }

  protected setOnlyDiff(v: boolean): void {
    this.onlyDiff.set(v);
    void this.goto(0);
  }

  protected goto(page: number): Promise<void> {
    this.page.set(page);
    return this.load();
  }

  private async load(): Promise<void> {
    const id = ++this.requestId;
    this.loading.set(true);
    const t = await this.notify.attempt(() =>
      this.api.properties.table(this.company(), {
        filter: this.filter().trim(),
        onlyDifferences: this.onlyDiff(),
        page: this.page(),
        size: SIZE,
      }),
    );
    if (id !== this.requestId) return; // veraltete Antwort
    this.loading.set(false);
    if (t) this.table.set(t);
  }

  /** Angezeigter Wert: ungespeicherte Änderung, sonst Serverwert. */
  protected shown(col: string, key: string, orig: string | null): string | null {
    const e = this.edits().get(col + SEP + key);
    return e === undefined ? orig : e;
  }

  protected edit(col: string, key: string, orig: string | null, value: string): void {
    // Leeres Feld bei fehlendem Schlüssel = keine Änderung
    this.setEdit(col, key, orig, orig === null && value === '' ? null : value);
  }

  protected remove(col: string, key: string, orig: string | null): void {
    this.setEdit(col, key, orig, null);
  }

  protected undo(col: string, key: string): void {
    this.setEdit(col, key, undefined, undefined);
  }

  private setEdit(col: string, key: string, orig: string | null | undefined, value: string | null | undefined): void {
    const next = new Map(this.edits());
    const k = col + SEP + key;
    // gleich wie Original (inkl. "fehlt" -> "fehlt") => keine Änderung
    if (value === undefined || value === orig) next.delete(k);
    else next.set(k, value);
    this.edits.set(next);
  }

  protected discard(): void {
    this.edits.set(new Map());
  }

  protected async save(): Promise<void> {
    const changes: PropertyChange[] = [...this.edits()].map(([k, value]) => {
      const [column, key] = k.split(SEP);
      return { column, key, value };
    });
    this.saving.set(true);
    const res = await this.notify.attempt(() => this.api.properties.change(this.company(), changes));
    this.saving.set(false);
    if (res) {
      this.notify.notify(`${res.applied} Änderung(en) gespeichert`);
      this.edits.set(new Map());
      await this.load();
    }
  }
}
