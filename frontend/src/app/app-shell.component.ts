import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppComponent } from './app.component';
import { TournamentComponent } from './tournament/tournament.component';

@Component({
  selector:'app-root',standalone:true,imports:[AppComponent,TournamentComponent],
  template:"@if (analytics) { <app-analytics [readOnly]=\"readOnly()\"/> } @else { <app-tournaments [readOnly]=\"readOnly()\"/> }"
})
export class AppShellComponent {
  readonly analytics=new URLSearchParams(window.location.search).get('modulo')==='analisis';
  readonly readOnly = signal(true);

  constructor() {
    inject(HttpClient).get<{ demo: boolean }>('/api/health')
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (health) => this.readOnly.set(health.demo), error: () => {} });
  }
}
