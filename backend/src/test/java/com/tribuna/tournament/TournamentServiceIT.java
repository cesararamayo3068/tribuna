package com.tribuna.tournament;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static com.tribuna.tournament.TournamentModels.*;

@SpringBootTest(properties="tribuna.demo.enabled=false")
@Transactional
class TournamentServiceIT {
    @Autowired TournamentService service;
    private UUID tournament;
    @BeforeEach void setup() {
        tournament=service.create(new CreateTournament("Copa de prueba","2026","Sede de prueba")).tournament().id();
        for(String name:List.of("Pibes","Norte","Sur","Oeste")) service.addTeam(tournament,new CreateTeam(name,name.substring(0,3),"#507b63"));
    }
    private ResultRequest result(int home,int away,long version) { return new ResultRequest(BigDecimal.valueOf(home),BigDecimal.valueOf(away),version); }
    @Test void persistsFixtureAndStandingsAndRecalculatesCorrections() {
        Detail generated=service.generateFixture(tournament);
        assertThat(generated.matches()).hasSize(6);
        Match match=generated.matches().getFirst();
        Detail scored=service.result(tournament,match.id(),result(3,1,match.version()));
        assertThat(scored.tournament().playedMatches()).isEqualTo(1);
        assertThat(scored.standings().getFirst().teamId()).isEqualTo(match.homeId());
        Match current=scored.matches().stream().filter(m -> m.id().equals(match.id())).findFirst().orElseThrow();
        Detail corrected=service.result(tournament,match.id(),result(0,2,current.version()));
        assertThat(corrected.standings().getFirst().teamId()).isEqualTo(match.awayId());
        assertThat(corrected.standings().getFirst().points()).isEqualTo(3);
    }
    @Test void preventsDuplicateFixturesAndRosterChangesAfterGeneration() {
        service.generateFixture(tournament);
        assertThatThrownBy(() -> service.generateFixture(tournament)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("ya tiene un fixture");
        assertThatThrownBy(() -> service.addTeam(tournament,new CreateTeam("Nuevo","NUE","#123456"))).hasMessageContaining("inscripción");
    }
    @Test void rejectsEquivalentNames() {
        assertThatThrownBy(() -> service.addTeam(tournament,new CreateTeam("  pÍbes  ","PIB","#123456"))).hasMessageContaining("Ya hay un equipo");
    }
    @Test void detectsStaleResultVersions() {
        Match match=service.generateFixture(tournament).matches().getFirst();
        service.result(tournament,match.id(),result(1,0,0));
        assertThatThrownBy(() -> service.result(tournament,match.id(),result(2,0,0))).hasMessageContaining("cambió");
    }
    @Test void refusesDecimalGoalsAndPartialScores() {
        Match match=service.generateFixture(tournament).matches().getFirst();
        assertThatThrownBy(() -> service.result(tournament,match.id(),new ResultRequest(new BigDecimal("1.5"),BigDecimal.ZERO,0L))).hasMessageContaining("enteros");
        assertThatThrownBy(() -> service.result(tournament,match.id(),new ResultRequest(BigDecimal.ONE,null,0L))).hasMessageContaining("ambos equipos");
    }
    @Test void clearingResultRestoresPendingStatusAndZeroPoints() {
        Match match=service.generateFixture(tournament).matches().getFirst();
        service.result(tournament,match.id(),result(1,0,0));
        Detail cleared=service.result(tournament,match.id(),new ResultRequest(null,null,1L));
        assertThat(cleared.tournament().playedMatches()).isZero();
        assertThat(cleared.standings()).allSatisfy(row -> assertThat(row.points()).isZero());
    }
    @Test void preventsOverlappingFieldsAndAllowsExactlyNinetyMinutes() {
        List<Match> matches=service.generateFixture(tournament).matches();
        Match first=matches.getFirst();
        Match other=matches.stream().filter(m -> !Set.of(first.homeId(),first.awayId()).contains(m.homeId()) && !Set.of(first.homeId(),first.awayId()).contains(m.awayId())).findFirst().orElseThrow();
        service.schedule(tournament,first.id(),new ScheduleRequest("2035-01-01T15:00:00Z","Cancha de prueba",0L));
        assertThatThrownBy(() -> service.schedule(tournament,other.id(),new ScheduleRequest("2035-01-01T15:30:00Z","Cancha de prueba",0L))).hasMessageContaining("superpone");
        assertThat(service.schedule(tournament,other.id(),new ScheduleRequest("2035-01-01T16:30:00Z","Cancha de prueba",0L)).matches()).hasSize(6);
    }
    @Test void onlyRemovesTeamsBeforeFixture() {
        Team team=service.detail(tournament).teams().getFirst();
        assertThat(service.removeTeam(tournament,team.id()).teams()).hasSize(3);
        assertThat(service.generateFixture(tournament).matches()).hasSize(3);
        assertThatThrownBy(() -> service.removeTeam(tournament,service.detail(tournament).teams().getFirst().id())).hasMessageContaining("después");
    }
}
