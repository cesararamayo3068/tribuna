import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { IconComponent } from '../icon.component';
import { TournamentSummary, TournamentDetail, TournamentTeam, TournamentMatch } from './tournament.model';

type Section = 'overview' | 'teams' | 'fixture' | 'standings';
type Modal = 'tournament' | 'team' | 'generate' | 'result' | 'schedule' | 'remove';

@Component({
  selector:'app-tournaments', standalone:true, imports:[CommonModule,FormsModule,IconComponent],
  templateUrl:'./tournament.component.html', styleUrl:'./tournament.component.css'
})
export class TournamentComponent {
  private readonly http=inject(HttpClient);
  private readonly destroyRef=inject(DestroyRef);
  readonly modal=viewChild.required<ElementRef<HTMLDialogElement>>('modal');
  readonly tournaments=signal<TournamentSummary[]>([]);
  readonly detail=signal<TournamentDetail|null>(null);
  readonly selectedId=signal('');
  readonly section=signal<Section>('overview');
  readonly loading=signal(true);
  readonly saving=signal(false);
  readonly error=signal('');
  readonly modalError=signal('');
  readonly conflict=signal(false);
  readonly notice=signal('');
  readonly modalType=signal<Modal>('tournament');
  readonly selectedMatch=signal<TournamentMatch|null>(null);
  readonly selectedTeam=signal<TournamentTeam|null>(null);
  readonly round=signal(1);
  readonly matchFilter=signal('all');
  readonly teamSearch=signal('');
  readonly pageTitle=computed(()=>({overview:'El torneo, en tus manos',teams:'Los protagonistas',fixture:'Cada cruce cuenta',standings:'La tabla habla'})[this.section()]);
  readonly rounds=computed(()=>Array.from({length:Math.max(0,...(this.detail()?.matches.map(m=>m.round)??[]))},(_,i)=>i+1));
  readonly currentMatches=computed(()=>(this.detail()?.matches??[]).filter(m=>m.round===this.round()&&(this.matchFilter()==='all'||(this.matchFilter()==='played')===this.played(m))));
  readonly nextMatches=computed(()=>(this.detail()?.matches??[]).filter(m=>!this.played(m)).sort((a,b)=>a.round-b.round||this.dateOrder(a)-this.dateOrder(b)||a.homeName.localeCompare(b.homeName)).slice(0,4));
  readonly goalCount=computed(()=>(this.detail()?.matches??[]).reduce((total,m)=>total+(m.homeGoals??0)+(m.awayGoals??0),0));
  readonly filteredTeams=computed(()=>{const q=this.normalize(this.teamSearch());return(this.detail()?.teams??[]).filter(team=>this.normalize(team.name).includes(q));});
  readonly restTeam=computed(()=>{
    const d=this.detail();if(!d||d.teams.length%2===0||!d.tournament.fixtureGenerated)return null;
    const playing=new Set(d.matches.filter(m=>m.round===this.round()).flatMap(m=>[m.homeId,m.awayId]));
    return d.teams.find(t=>!playing.has(t.id))??null;
  });
  readonly palette=['#507b63','#b76850','#7472a8','#b8964e','#529b91','#597eab','#a35a72','#bb814f'];
  tournamentName=''; season=String(new Date().getFullYear()); location='';
  teamName=''; shortName=''; teamColor='#507b63';
  homeGoals:number|null=null; awayGoals:number|null=null;
  kickoffLocal=''; field='';
  constructor(){this.loadList();}
  loadList():void {
    this.loading.set(true);this.error.set('');
    this.http.get<TournamentSummary[]>('/api/tournaments').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:list=>{
        this.tournaments.set(list);
        const remembered=localStorage.getItem('tribuna.selectedTournament');
        const id=list.find(t=>t.id===this.selectedId())?.id??list.find(t=>t.id===remembered)?.id??list[0]?.id;
        if(id)this.selectTournament(id);else{this.detail.set(null);this.loading.set(false);}
      },
      error:()=>{this.loading.set(false);this.error.set('No pudimos cargar los torneos. Volvé a intentar en unos instantes.');}
    });
  }
  selectTournament(id:string):void {
    this.selectedId.set(id);this.loading.set(true);this.error.set('');this.notice.set('');this.matchFilter.set('all');this.teamSearch.set('');
    this.http.get<TournamentDetail>('/api/tournaments/'+encodeURIComponent(id)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:d=>{if(this.selectedId()!==id)return;this.accept(d);this.loading.set(false);this.round.set(d.matches.find(m=>!this.played(m))?.round??1);},
      error:()=>{if(this.selectedId()!==id)return;this.loading.set(false);this.detail.set(null);this.error.set('No pudimos abrir ese torneo. Intentá de nuevo.');}
    });
  }
  navigate(section:Section):void{this.section.set(section);this.notice.set('');this.teamSearch.set('');}
  played(match:TournamentMatch):boolean{return match.homeGoals!==null&&match.awayGoals!==null;}
  open(type:Modal,match:TournamentMatch|null=null,team:TournamentTeam|null=null):void {
    this.modalType.set(type);this.modalError.set('');this.conflict.set(false);this.selectedMatch.set(match);this.selectedTeam.set(team);
    if(type==='tournament'){this.tournamentName='';this.season=String(new Date().getFullYear());this.location='';}
    if(type==='team'){this.teamName='';this.shortName='';this.teamColor=this.palette[(this.detail()?.teams.length??0)%this.palette.length];}
    if(type==='result'){this.homeGoals=match?.homeGoals??null;this.awayGoals=match?.awayGoals??null;}
    if(type==='schedule'){
      this.kickoffLocal=match?.kickoffAt?this.toLocalInput(match.kickoffAt):'';
      this.field=match?.field??this.detail()?.tournament.location??'';
    }
    this.modal().nativeElement.showModal();
  }
  close():void{if(!this.saving())this.modal().nativeElement.close();}
  cancel(event:Event):void{if(this.saving())event.preventDefault();}
  createTournament():void{
    this.round.set(1);this.matchFilter.set('all');
    this.mutate(this.http.post<TournamentDetail>('/api/tournaments',{name:this.tournamentName,season:this.season,location:this.location}),
      'Torneo creado. Ahora inscribí a los equipos.','teams');
  }
  addTeam():void {
    const id=this.selectedId();if(!id)return;
    this.mutate(this.http.post<TournamentDetail>('/api/tournaments/'+id+'/teams',{name:this.teamName,shortName:this.shortName,color:this.teamColor}),'Equipo inscrito.');
  }
  removeTeam():void{
    const team=this.selectedTeam();if(!team)return;
    this.mutate(this.http.delete<TournamentDetail>('/api/tournaments/'+this.selectedId()+'/teams/'+team.id),'Equipo retirado de la inscripción.');
  }
  generate():void{
    this.mutate(this.http.post<TournamentDetail>('/api/tournaments/'+this.selectedId()+'/fixture',{}),'Fixture generado. Ya podés programar y jugar los partidos.','fixture');
  }
  saveResult(clear=false):void {
    const match=this.selectedMatch();if(!match)return;
    if(!clear && (this.homeGoals===null||this.awayGoals===null||!Number.isInteger(this.homeGoals)||!Number.isInteger(this.awayGoals)||this.homeGoals<0||this.awayGoals<0||this.homeGoals>99||this.awayGoals>99)){
      this.modalError.set('Completá los goles de ambos equipos con enteros entre 0 y 99.');return;
    }
    this.mutate(this.http.put<TournamentDetail>('/api/tournaments/'+this.selectedId()+'/matches/'+match.id+'/result',
      {homeGoals:clear?null:this.homeGoals,awayGoals:clear?null:this.awayGoals,version:match.version}),
      clear?'Resultado retirado. La tabla se recalculó.':'Resultado guardado. La tabla ya está actualizada.');
  }
  saveSchedule(clear=false):void{
    const match=this.selectedMatch();if(!match)return;
    let kickoffAt:string|null=null;
    if(!clear){
      const date=new Date(this.kickoffLocal);
      if(!this.kickoffLocal||Number.isNaN(date.getTime())||!this.field.trim()){this.modalError.set('Indicá una fecha, una hora y una cancha.');return;}
      kickoffAt=date.toISOString();
    }
    this.mutate(this.http.put<TournamentDetail>('/api/tournaments/'+this.selectedId()+'/matches/'+match.id+'/schedule',
      {kickoffAt,field:clear?null:this.field,version:match.version}),clear?'Programación retirada.':'Fecha, hora y cancha actualizadas.');
  }
  refreshCurrent():void { this.close();this.selectTournament(this.selectedId()); }
  teamPoints(id:string):number{return this.detail()?.standings.find(s=>s.teamId===id)?.points??0;}
  private mutate(request:Observable<TournamentDetail>,message:string,section?:Section):void{
    if(this.saving())return;
    this.saving.set(true);this.modalError.set('');this.conflict.set(false);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:d=>{
        this.accept(d);this.saving.set(false);this.close();this.error.set('');this.notice.set(message);
        if(section)this.section.set(section);
        this.round.set(Math.min(Math.max(this.round(),1),Math.max(1,...d.matches.map(m=>m.round))));
        this.http.get<TournamentSummary[]>('/api/tournaments').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:list=>this.tournaments.set(list),error:()=>{}});
      },
      error:(error:HttpErrorResponse)=>{this.saving.set(false);this.modalError.set(error.error?.message??'No pudimos guardar los cambios. Intentá de nuevo.');this.conflict.set(error.status===409);}
    });
  }
  private accept(d:TournamentDetail):void{
    this.detail.set(d);this.selectedId.set(d.tournament.id);
    localStorage.setItem('tribuna.selectedTournament',d.tournament.id);
    this.tournaments.update(list=>[d.tournament,...list.filter(t=>t.id!==d.tournament.id)]);
  }
  private dateOrder(match:TournamentMatch):number{return match.kickoffAt?new Date(match.kickoffAt).getTime():Number.MAX_SAFE_INTEGER;}
  private normalize(value:string):string{return value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();}
  private toLocalInput(value:string):string {
    const d=new Date(value);const pad=(n:number)=>String(n).padStart(2,'0');
    return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())+'T'+pad(d.getHours())+':'+pad(d.getMinutes());
  }
}
