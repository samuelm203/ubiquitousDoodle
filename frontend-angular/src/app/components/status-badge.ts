import { Component, computed, input } from '@angular/core';
import { STATUS, Tone } from '../format';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge badge-' + info().tone">{{ info().label }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<string>();

  protected readonly info = computed<{ label: string; tone: Tone }>(
    () => (STATUS as Record<string, { label: string; tone: Tone }>)[this.status()] ?? { label: this.status(), tone: 'neutral' },
  );
}
