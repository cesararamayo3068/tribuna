import { Component, DestroyRef, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Report, HistoryItem } from './analysis.model';
import { IconComponent } from './icon.component';

type View = 'summary' | 'teams' | 'members' | 'history';

@Component({
  selector: 'app-analytics', standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  templateUrl: './app.component.html', styleUrl: './app.component.css'
})
export class AppComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  readonly readOnly = input(false);
  readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('uploadDialog');
  readonly report = signal<Report | null>(null);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly error = signal('');
  readonly uploadError = signal('');
  readonly selectedFile = signal<File | null>(null);
  readonly dragging = signal(false);
  readonly activeView = signal<View>('summary');
  readonly history = signal<HistoryItem[]>([]);
  readonly historyError = signal('');
  readonly query = signal('');
  readonly order = signal('members');
  readonly page = signal(0);
  readonly pageSize = 10;
  readonly title = computed(() => ({ summary: 'Resumen general', teams: 'Todos los equipos', members: 'Conocé a los socios', history: 'Tus análisis' })[this.activeView()]);
  readonly filteredTeams = computed(() => {
    const search = this.normalize(this.query());
    const teams = (this.report()?.teams ?? []).filter(t => this.normalize(t.team).includes(search));
    return [...teams].sort((a,b) => this.order() === 'age' ? b.averageAge - a.averageAge || a.team.localeCompare(b.team) : b.members - a.members || a.team.localeCompare(b.team));
  });
  readonly filteredMembers = computed(() => {
    const search = this.normalize(this.query());
    return (this.report()?.marriedUniversity ?? []).filter(m => this.normalize(m.name + ' ' + m.team).includes(search));
  });
  readonly pageMembers = computed(() => this.filteredMembers().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize));
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredMembers().length / this.pageSize)));
  readonly maxAgeCount = computed(() => Math.max(1, ...(this.report()?.ages.map(a => a.count) ?? [])));
  readonly maxTeamCount = computed(() => Math.max(1, this.report()?.teams[0]?.members ?? 1));

  constructor() { this.loadDemo(); this.loadHistory(); }

  navigate(view: View): void {
    this.activeView.set(view); this.query.set(''); this.page.set(0);
    if (view === 'history') this.loadHistory();
  }
  loadDemo(): void {
    this.loading.set(true); this.error.set('');
    this.http.get<Report>('/api/analyses/demo').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: report => { this.report.set(report); this.loading.set(false); this.navigate('summary'); },
      error: () => { this.loading.set(false); this.error.set('No pudimos conectar con Tribuna. Verificá que el servidor esté activo y volvé a intentar.'); }
    });
  }
  loadHistory(): void {
    this.historyError.set('');
    this.http.get<HistoryItem[]>('/api/analyses').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => this.history.set(items),
      error: () => this.historyError.set('No pudimos cargar el historial. Intentá de nuevo.')
    });
  }
  openAnalysis(id: string): void {
    this.loading.set(true); this.error.set('');
    this.http.get<Report>('/api/analyses/' + encodeURIComponent(id)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: report => { this.report.set(report); this.loading.set(false); this.navigate('summary'); },
      error: () => { this.loading.set(false); this.error.set('No pudimos abrir ese análisis. Intentá nuevamente.'); }
    });
  }
  openUpload(): void {
    if (this.readOnly()) return;
    this.selectedFile.set(null); this.uploadError.set(''); this.dragging.set(false);
    this.dialog().nativeElement.showModal();
  }
  closeUpload(): void { if (!this.uploading()) this.dialog().nativeElement.close(); }
  onCancel(event: Event): void { if (this.uploading()) event.preventDefault(); }
  pickFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.selectFile(file);
    input.value = '';
  }
  dragOver(event: DragEvent): void { event.preventDefault(); if (!this.uploading()) this.dragging.set(true); }
  drop(event: DragEvent): void {
    event.preventDefault(); this.dragging.set(false);
    if (this.uploading()) return;
    const files = event.dataTransfer?.files;
    if (files?.length !== 1) { this.uploadError.set('Cargá un solo archivo por vez.'); return; }
    this.selectFile(files[0]);
  }
  private selectFile(file: File): void {
    this.selectedFile.set(null); this.uploadError.set('');
    if (!file.name.toLowerCase().endsWith('.csv')) { this.uploadError.set('Necesitamos un archivo .csv. Podés descargar el ejemplo para ver el formato.'); return; }
    if (file.size === 0 || file.size > 5 * 1024 * 1024) { this.uploadError.set('El archivo debe tener contenido y pesar como máximo 5 MB.'); return; }
    this.selectedFile.set(file);
  }
  upload(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) return;
    const body = new FormData(); body.append('file', file);
    this.uploading.set(true); this.uploadError.set('');
    this.http.post<Report>('/api/analyses', body).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: report => {
        this.report.set(report); this.error.set(''); this.uploading.set(false);
        this.dialog().nativeElement.close(); this.navigate('summary'); this.loadHistory();
      },
      error: (error: HttpErrorResponse) => {
        this.uploading.set(false);
        this.uploadError.set(error.error?.message ?? 'No pudimos analizar el archivo. Revisá la conexión y volvé a intentar.');
      }
    });
  }
  search(value: string): void { this.query.set(value); this.page.set(0); }
  percent(count: number): number { return this.report() ? count / this.report()!.totalMembers * 100 : 0; }
  teamCode(team: string): string {
    const codes: Record<string,string> = { River:'RIV', Boca:'BOC', Racing:'RAC', Independiente:'IND', 'San Lorenzo':'SLO', 'Newell’s':'NOB', 'Huracán':'HUR', 'Vélez':'VEL', Estudiantes:'EST', 'Gimnasia LP':'GIM', 'Rosario Central':'ROS' };
    return codes[team] ?? team.slice(0,3).toUpperCase();
  }
  teamColor(team: string): string {
    const colors: Record<string,string> = { River:'#b5463c', Boca:'#375d9f', Racing:'#589db6', Independiente:'#b84540', 'San Lorenzo':'#74518a', 'Newell’s':'#9c4d4b', 'Huracán':'#b85c5c', 'Vélez':'#526c94' };
    return colors[team] ?? '#547768';
  }
  exportTeams(): void {
    const report = this.report(); if (!report) return;
    const escape = (value: string | number) => {
      let text = String(value); if (/^[=+@\-\t\r]/.test(text)) text = "'" + text;
      return '"' + text.replaceAll('"','""') + '"';
    };
    const rows: (string | number)[][] = [['Equipo','Socios','Promedio de edad','Edad mínima','Edad máxima'], ...report.teams.map(t => [t.team,t.members,t.averageAge,t.youngestAge,t.oldestAge])];
    const blob = new Blob(['\uFEFF' + rows.map(row=>row.map(escape).join(';')).join('\r\n')], {type:'text/csv;charset=utf-8'});
    const url = URL.createObjectURL(blob); const link = document.createElement('a');
    link.href = url; link.download = 'tribuna-equipos.csv'; link.click(); URL.revokeObjectURL(url);
  }
  private normalize(value: string): string { return value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase(); }
}
