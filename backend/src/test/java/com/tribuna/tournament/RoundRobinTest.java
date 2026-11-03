package com.tribuna.tournament;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.tribuna.tournament.TournamentModels.*;

class RoundRobinTest {
    @Test void everyPairPlaysExactlyOnceForEvenAndOddTournaments() {
        for(int count=2;count<=20;count++) {
            List<UUID> teams=new ArrayList<>();
            for(int i=0;i<count;i++) teams.add(UUID.randomUUID());
            List<Pairing> matches=RoundRobin.generate(teams);
            assertThat(matches).hasSize(count*(count-1)/2);
            Set<Set<UUID>> pairs=new HashSet<>();
            for(Pairing match:matches) {
                assertThat(match.homeId()).isNotEqualTo(match.awayId());
                assertThat(pairs.add(Set.of(match.homeId(),match.awayId()))).isTrue();
            }
            for(UUID team:teams) assertThat(matches.stream().filter(m -> m.homeId().equals(team)||m.awayId().equals(team)).count()).isEqualTo(count-1);
            int rounds=count%2==0?count-1:count;
            for(int round=1;round<=rounds;round++) {
                int current=round;
                var participants=matches.stream().filter(m -> m.round()==current).flatMap(m -> java.util.stream.Stream.of(m.homeId(),m.awayId())).toList();
                assertThat(participants).doesNotHaveDuplicates();
                assertThat(participants).hasSize(count%2==0?count:count-1);
            }
        }
    }
    @Test void generationDoesNotModifyInputAndIsDeterministic() {
        var teams=List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
        assertThat(RoundRobin.generate(teams)).isEqualTo(RoundRobin.generate(teams));
        assertThat(teams).hasSize(3);
    }
    @Test void rejectsTooFewTeamsAndDuplicates() {
        UUID id=UUID.randomUUID();
        assertThatThrownBy(() -> RoundRobin.generate(List.of(id))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoundRobin.generate(List.of(id,id))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoundRobin.generate(Arrays.asList(id,null))).isInstanceOf(IllegalArgumentException.class);
    }
}
