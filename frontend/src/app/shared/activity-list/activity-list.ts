import { Component, input } from '@angular/core';

export interface ElementActivite {
  icon: string;
  tone: 'orange' | 'green';
  titre: string;
  detail: string;
  temps: string;
}

@Component({
  selector: 'app-activity-list',
  template: `
    <div class="mt-1">
      @if (items().length === 0) {
        <p class="border-b border-line px-1 py-6 text-center text-[13px] text-gray-500">Aucune activité récente.</p>
      } @else {
        @for (item of items(); track item.titre) {
        <div class="flex items-center gap-3 border-b border-line py-3.5 px-1 last:border-b-0">
          <span class="grid h-9 w-9 place-items-center rounded-full text-lg" [class]="item.tone === 'orange' ? 'bg-[#f8dbd2] text-[#b9513f]' : 'bg-[#dcebc8] text-[#587145]'">
            {{ item.icon }}
          </span>
          <div class="flex-1">
            <strong class="block text-xs">{{ item.titre }}</strong>
            <small class="mt-1 block text-[11px] text-gray-500">{{ item.detail }}</small>
          </div>
          <time class="text-[11px] text-gray-500">{{ item.temps }}</time>
        </div>
        }
      }
    </div>
  `,
})
export class ActivityList {
  readonly items = input.required<ElementActivite[]>();
}
