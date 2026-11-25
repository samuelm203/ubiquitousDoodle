import { Component, inject } from '@angular/core';
import { Notify } from '../notify';

@Component({
  selector: 'app-toast-stack',
  template: `
    <div class="toasts" aria-live="polite">
      @for (t of notify.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.kind">{{ t.message }}</div>
      }
    </div>
  `,
})
export class ToastStack {
  protected readonly notify = inject(Notify);
}
