import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-feature-card',
  imports: [RouterLink],
  template: `
    <article class="relative min-h-[260px] overflow-hidden rounded-lg p-7" [class]="fondClasse()">
      <p class="mb-8 font-mono text-[10px] font-medium uppercase tracking-[.12em]">{{ kicker() }}</p>
      <h3 class="relative z-10 max-w-[320px] font-display text-[25px] font-semibold leading-[1.08]">{{ title() }}</h3>
      <p class="relative z-10 mt-3 max-w-[340px] text-xs leading-relaxed" [class]="tone() === 'dark' ? 'text-[#aeb9a7]' : 'text-[#60705d]'">
        {{ description() }}
      </p>
      <a [routerLink]="link()" class="relative z-10 text-[11px] font-extrabold no-underline" [class]="tone() === 'dark' ? 'text-orange-400' : 'text-ink'">
        {{ action() }} <span class="ml-1 text-base">↗</span>
      </a>
      <div class="pointer-events-none absolute -bottom-4 right-6 text-[145px] leading-none opacity-70"
           [class]="tone() === 'dark' ? 'text-white/10' : 'text-ink/10'">
        {{ tone() === 'dark' ? '✳' : '◎' }}
      </div>
    </article>
  `,
})
export class FeatureCard {
  readonly kicker = input.required<string>();
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly action = input.required<string>();
  readonly link = input.required<string>();
  readonly tone = input<'dark' | 'light'>('dark');

  protected fondClasse(): string {
    return this.tone() === 'dark' ? 'bg-ink text-[#f6f7ef]' : 'bg-[#e6edcf] text-ink';
  }
}
