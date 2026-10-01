import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  template: `
    <article class="relative min-h-[125px] overflow-hidden rounded-lg p-5" [class]="fondClasse()">
      <strong class="block font-display text-[37px] font-bold text-ink">{{ value() }}</strong>
      <span class="mt-1.5 block text-xs text-[#536052]">{{ label() }}</span>
      <i class="pointer-events-none absolute -bottom-8 -right-4 h-[100px] w-[100px] rounded-full border-[14px] border-white/40"></i>
    </article>
  `,
})
export class StatCard {
  readonly value = input.required<string>();
  readonly label = input.required<string>();
  readonly tone = input<'green' | 'yellow' | 'coral'>('green');

  protected fondClasse(): string {
    switch (this.tone()) {
      case 'yellow':
        return 'bg-[#f1e5bc]';
      case 'coral':
        return 'bg-[#f2cfc5]';
      default:
        return 'bg-[#e5eadb]';
    }
  }
}
