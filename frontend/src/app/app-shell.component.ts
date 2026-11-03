import { Component } from '@angular/core';
import { AppComponent } from './app.component';
import { TournamentComponent } from './tournament/tournament.component';

@Component({
  selector:'app-root',standalone:true,imports:[AppComponent,TournamentComponent],
  template:"@if (analytics) { <app-analytics/> } @else { <app-tournaments/> }"
})
export class AppShellComponent {
  readonly analytics=new URLSearchParams(window.location.search).get('modulo')==='analisis';
}
