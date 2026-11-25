import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal-dialog',
  host: { '(window:keydown.escape)': 'closed.emit()' },
  template: `
    <div class="modal-backdrop" (mousedown)="onBackdrop($event)">
      <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="title()">
        <header class="modal-head">
          <h2>{{ title() }}</h2>
          <button type="button" class="btn btn-ghost btn-icon" aria-label="Schliessen" (click)="closed.emit()">✕</button>
        </header>
        <ng-content />
      </div>
    </div>
  `,
})
export class ModalDialog {
  readonly title = input.required<string>();
  readonly closed = output<void>();

  protected onBackdrop(e: MouseEvent): void {
    if (e.target === e.currentTarget) this.closed.emit();
  }
}
