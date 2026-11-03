package com.tribuna.tournament;

import static com.tribuna.tournament.TournamentModels.*;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TournamentService {
    private final TournamentRepository repository;
    private static final UUID DEMO_ID = UUID.fromString("c0ba2026-0000-4000-8000-000000000001");
    public TournamentService(TournamentRepository repository) { this.repository = repository; }

    @Transactional(readOnly=true)
    public List<Summary> list() { return repository.list(); }
    @Transactional(readOnly=true)
    public Detail detail(UUID id) {
        Summary summary = repository.summary(id).orElseThrow(() -> missing("No encontramos ese torneo."));
        List<Team> teams = repository.teams(id);
        List<Match> matches = repository.matches(id);
        return new Detail(summary,teams,matches,StandingsCalculator.calculate(teams,matches));
    }
    @Transactional
    public Detail create(CreateTournament input) {
        if (input == null) throw bad("Completá los datos del torneo.");
        String name = text(input.name(),"nombre del torneo",80);
        String season = text(input.season(),"temporada",30);
        String location = text(input.location(),"sede",100);
        UUID id=UUID.randomUUID();
        repository.insertTournament(id,name,season,location);
        return detail(id);
    }
    @Transactional
    public Detail addTeam(UUID tournament, CreateTeam input) {
        lock(tournament);
        Detail current=detail(tournament);
        if (current.tournament().fixtureGenerated()) throw conflict("El fixture ya está generado. La inscripción de equipos está cerrada.");
        if (current.teams().size() >= 20) throw bad("El máximo es de 20 equipos por torneo.");
        if (input == null) throw bad("Completá los datos del equipo.");
        String name=text(input.name(),"nombre del equipo",60);
        String normalized=key(name);
        if (normalized.isEmpty()) throw bad("El nombre del equipo debe incluir letras o números.");
        if (repository.duplicateName(tournament,normalized)) throw conflict("Ya hay un equipo con ese nombre en este torneo.");
        String shortName=text(input.shortName(),"abreviatura",5).toUpperCase(Locale.ROOT);
        if (!shortName.matches("[A-Z0-9]{2,5}")) throw bad("La abreviatura debe tener de 2 a 5 letras o números.");
        if (input.color()==null || !input.color().matches("#[a-fA-F0-9]{6}")) throw bad("Elegí un color válido para el equipo.");
        repository.insertTeam(UUID.randomUUID(),tournament,name,normalized,shortName,input.color());
        return detail(tournament);
    }
    @Transactional
    public Detail removeTeam(UUID tournament, UUID team) {
        lock(tournament);
        if (detail(tournament).tournament().fixtureGenerated()) throw conflict("No se pueden quitar equipos después de generar el fixture.");
        if (repository.deleteTeam(tournament,team)==0) throw missing("No encontramos ese equipo en el torneo.");
        return detail(tournament);
    }
    @Transactional
    public Detail generateFixture(UUID tournament) {
        lock(tournament);
        Detail current=detail(tournament);
        if (current.tournament().fixtureGenerated()) throw conflict("El torneo ya tiene un fixture. No se generó uno nuevo.");
        if (current.teams().size()<2) throw bad("Inscribí al menos 2 equipos para generar el fixture.");
        RoundRobin.generate(current.teams().stream().map(Team::id).toList()).forEach(pair -> repository.insertMatch(tournament,pair));
        return detail(tournament);
    }
    @Transactional
    public Detail result(UUID tournament, UUID matchId, ResultRequest input) {
        lock(tournament);
        Match match=getMatch(tournament,matchId);
        if (input==null) throw bad("Completá el resultado.");
        checkVersion(match,input.version());
        Integer home=goals(input.homeGoals()), away=goals(input.awayGoals());
        if ((home==null)!=(away==null)) throw bad("Completá los goles de ambos equipos.");
        if (home!=null && (home<0 || home>99 || away<0 || away>99)) throw bad("Los goles deben ser enteros entre 0 y 99.");
        if (repository.updateResult(tournament,matchId,home,away,input.version())!=1) throw stale();
        return detail(tournament);
    }
    @Transactional
    public Detail schedule(UUID tournament, UUID matchId, ScheduleRequest input) {
        lock(tournament);
        repository.lockScheduling();
        Match match=getMatch(tournament,matchId);
        if (input==null) throw bad("Completá la programación del partido.");
        checkVersion(match,input.version());
        Instant kickoff=null;
        String field=null,fieldKey=null;
        if (input.kickoffAt()!=null && !input.kickoffAt().isBlank()) {
            try { kickoff=OffsetDateTime.parse(input.kickoffAt()).toInstant(); }
            catch (DateTimeException e) { throw bad("La fecha del partido no tiene un formato válido."); }
            field=text(input.field(),"cancha",100);
            fieldKey=key(field);
            if (fieldKey.isEmpty()) throw bad("La cancha debe incluir letras o números.");
            if (repository.scheduleConflict(match,kickoff,fieldKey)) throw conflict("Ese horario se superpone con otro partido del equipo o de la cancha. Dejá al menos 90 minutos entre inicios.");
        } else if (input.field()!=null && !input.field().isBlank()) throw bad("Indicá una fecha y hora para reservar la cancha.");
        if (repository.updateSchedule(tournament,matchId,kickoff,field,fieldKey,input.version())!=1) throw stale();
        return detail(tournament);
    }
    @Transactional
    public void seedDemo() {
        repository.lockSeeding();
        if (repository.summary(DEMO_ID).isPresent()) return;
        repository.insertTournament(DEMO_ID,"Copa de Barrio","Apertura 2026","Complejo La Canchita");
        String[][] clubs={
            {"Barrio Norte","BNO","#b76850"},{"Los Pibes","LPI","#507b63"},
            {"Deportivo Sur","DSU","#7472a8"},{"La Esquina","LES","#b8964e"},
            {"Unión del Parque","UDP","#529b91"},{"Atlético Oeste","AOE","#597eab"},
            {"San Martín","SMT","#a35a72"},{"La Banda","LBA","#bb814f"}
        };
        for(String[] club:clubs) repository.insertTeam(UUID.randomUUID(),DEMO_ID,club[0],key(club[0]),club[1],club[2]);
        generateFixture(DEMO_ID);
        List<Match> matches=repository.matches(DEMO_ID);
        LocalDate start=LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(1);
        int[][] scores={{2,1},{0,0},{1,3},{2,2},{1,0},{3,1},{0,2},{2,1}};
        int index=0;
        for(Match match:matches) {
            int slot=(index%4)/2, pitch=index%2+1;
            Instant kickoff=start.plusWeeks(match.round()-1).atTime(15+slot*2,0)
                .atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();
            repository.updateSchedule(DEMO_ID,match.id(),kickoff,"La Canchita · Cancha "+pitch,key("La Canchita · Cancha "+pitch),0);
            if(index<scores.length) repository.updateResult(DEMO_ID,match.id(),scores[index][0],scores[index][1],1);
            index++;
        }
    }
    private static Integer goals(java.math.BigDecimal value) {
        if(value==null) return null;
        try { return value.intValueExact(); }
        catch(ArithmeticException e) { throw bad("Los goles deben ser enteros entre 0 y 99."); }
    }
    private void lock(UUID id) { if(!repository.lock(id)) throw missing("No encontramos ese torneo."); }
    private Match getMatch(UUID tournament,UUID id) {
        return repository.match(tournament,id).orElseThrow(() -> missing("No encontramos ese partido en el torneo."));
    }
    private void checkVersion(Match match,Long version) {
        if(version==null || version<0) throw bad("Falta la versión del partido. Actualizá el torneo e intentá de nuevo.");
        if(match.version()!=version) throw stale();
    }
    private static String text(String value,String field,int max) {
        String clean=value==null?"":value.strip().replaceAll("\\s+"," ");
        if(clean.isEmpty()) throw bad("Completá el campo "+field+".");
        if(clean.length()>max) throw bad("El campo "+field+" admite hasta "+max+" caracteres.");
        return clean;
    }
    static String key(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private static ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND,message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private static ResponseStatusException stale() { return conflict("El partido cambió desde que lo abriste. Actualizá el torneo antes de volver a guardar."); }
}
