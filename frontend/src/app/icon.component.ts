import { Component, Input } from '@angular/core';

@Component({
  selector: 't-icon', standalone: true,
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths[\'grid\']"/></svg>',
  styles: [':host{display:inline-flex;width:20px;height:20px;flex-shrink:0}svg{width:100%;height:100%}']
})
export class IconComponent {
  @Input() name = 'grid';
  readonly paths: Record<string,string> = {
    calendar: 'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2 M7 14h2 M15 14h2 M7 18h2',
    trophy: 'M7 3h10v6a5 5 0 0 1-10 0z M7 5H3v3a4 4 0 0 0 4 4 M17 5h4v3a4 4 0 0 1-4 4 M12 14v5 M7 22h10 M9 19h6v3H9z',
    pin: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0 M15 10a3 3 0 1 0-6 0 3 3 0 0 0 6 0',
    refresh: 'M20 8a9 9 0 0 0-15-3L2 8 M2 2v6h6 M4 16a9 9 0 0 0 15 3l3-3 M22 22v-6h-6',
    edit: 'M16 3l5 5 M3 21l5-1 13-13a2 2 0 0 0-5-5L3 15z',
    target: 'M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M17 12a5 5 0 1 0-10 0 5 5 0 0 0 10 0 M13 12a1 1 0 1 0-2 0 1 1 0 0 0 2 0',
    grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
    shield: 'M12 3l8 3v6c0 5-8 9-8 9S4 17 4 12V6z M9 12l2 2 4-4',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M20 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
    clock: 'M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M12 7v5l3 2',
    upload: 'M12 16V3 M7 8l5-5 5 5 M4 16v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4',
    download: 'M12 3v13 M7 11l5 5 5-5 M4 16v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4',
    arrow: 'M5 12h14 M13 6l6 6-6 6',
    chart: 'M4 20h16 M7 16V9 M12 16V4 M17 16v-5',
    file: 'M14 2H5a1 1 0 0 0-1 1v18a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V8z M14 2v6h6 M8 13h8 M8 17h5',
    search: 'M16 16l5 5 M18 10a8 8 0 1 0-16 0 8 8 0 0 0 16 0',
    info: 'M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M12 11v5 M12 7h.01',
    check: 'M20 6L9 17l-5-5',
    close: 'M6 6l12 12 M18 6L6 18',
    plus: 'M12 5v14 M5 12h14',
    ball: 'M21 12a9 9 0 1 0-18 0 9 9 0 0 0 18 0 M12 7l5 4-2 6H9l-2-6z M12 3v4 M3.5 9l3.5 2 M6 19l3-2 M18 19l-3-2 M20.5 9L17 11',
    external: 'M14 3h7v7 M21 3l-9 9 M10 3H4a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-6'
  };
}
