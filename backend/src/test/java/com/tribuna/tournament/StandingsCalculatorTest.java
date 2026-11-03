package com.tribuna.tournament;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.tribuna.tournament.TournamentModels.*;

class StandingsCalculatorTest {
    private final UUID tournament=UUID.randomUUID();
    private Team team(String name) { return new Team(UUID.randomUUID(),tournament,name,"ABC","#123456"); }
    private Match match(Team home,Team away,Integer h,Integer a,int round) {
        return new Match(UUID.randomUUID(),round,home.id(),home.name(),home.shortName(),home.color(),
            away.id(),away.name(),away.shortName(),away.color(),h,a,null,null,0);
    }
    @Test void pendingMatchesDoNotCountAsDraws() {
        Team a=team("A"),b=team("B");
        var table=StandingsCalculator.calculate(List.of(a,b),List.of(match(a,b,null,null,1)));
        assertThat(table).allSatisfy(row -> {assertThat(row.played()).isZero();assertThat(row.points()).isZero();});
    }
    @Test void countsWinsDrawsLossesAndGoals() {
        Team a=team("A"),b=team("B"),c=team("C");
        var table=StandingsCalculator.calculate(List.of(a,b,c),List.of(match(a,b,3,1,1),match(a,c,2,2,2),match(b,c,0,1,3)));
        var first=table.getFirst();
        assertThat(first.name()).isEqualTo("A");
        assertThat(first.points()).isEqualTo(4);
        assertThat(first.played()).isEqualTo(2);
        assertThat(first.goalsFor()).isEqualTo(5);
        assertThat(first.goalsAgainst()).isEqualTo(3);
        assertThat(first.form()).containsExactly("G","E");
        assertThat(table.getLast().lost()).isEqualTo(2);
    }
    @Test void correctingAResultRecalculatesInsteadOfAccumulating() {
        Team a=team("A"),b=team("B");
        assertThat(StandingsCalculator.calculate(List.of(a,b),List.of(match(a,b,1,0,1))).getFirst().name()).isEqualTo("A");
        var corrected=StandingsCalculator.calculate(List.of(a,b),List.of(match(a,b,0,2,1)));
        assertThat(corrected.getFirst().name()).isEqualTo("B");
        assertThat(corrected.getFirst().points()).isEqualTo(3);
        assertThat(corrected).allSatisfy(row -> assertThat(row.played()).isEqualTo(1));
    }
    @Test void goalDifferenceThenGoalsForThenNameBreakTies() {
        Team a=team("Alfa"),b=team("Beta"),c=team("Cero"),d=team("Delta");
        var table=StandingsCalculator.calculate(List.of(a,b,c,d),List.of(match(a,c,2,0,1),match(b,d,3,1,1)));
        assertThat(table.getFirst().name()).isEqualTo("Beta");
        var empty=StandingsCalculator.calculate(List.of(b,a),List.of());
        assertThat(empty.getFirst().name()).isEqualTo("Alfa");
    }
    @Test void formOnlyContainsLastFiveResultsInRoundOrder() {
        Team a=team("A"),b=team("B");
        List<Match> matches=new ArrayList<>();
        for(int round=7;round>=1;round--) matches.add(match(a,b,round%2,0,round));
        assertThat(StandingsCalculator.calculate(List.of(a,b),matches).getFirst().form()).containsExactly("G","E","G","E","G");
    }
}
