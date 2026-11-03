package com.tribuna.tournament;

import static com.tribuna.tournament.TournamentModels.*;
import java.util.*;

public final class StandingsCalculator {
    private StandingsCalculator() {}
    private static final class Row {
        final Team team;
        int played, won, drawn, lost, goalsFor, goalsAgainst, points;
        final List<String> form = new ArrayList<>();
        Row(Team team) { this.team = team; }
        void result(int scored, int conceded) {
            played++; goalsFor += scored; goalsAgainst += conceded;
            if (scored > conceded) { won++; points += 3; form.add("G"); }
            else if (scored == conceded) { drawn++; points++; form.add("E"); }
            else { lost++; form.add("P"); }
        }
    }
    public static List<Standing> calculate(List<Team> teams, List<Match> matches) {
        Map<UUID, Row> rows = new HashMap<>();
        teams.forEach(team -> rows.put(team.id(), new Row(team)));
        matches.stream().filter(Match::played)
            .sorted(Comparator.comparingInt(Match::round).thenComparing(Match::homeName))
            .forEach(match -> {
                Row home = rows.get(match.homeId()), away = rows.get(match.awayId());
                if (home == null || away == null) throw new IllegalArgumentException("El partido contiene un equipo ajeno al torneo.");
                home.result(match.homeGoals(), match.awayGoals());
                away.result(match.awayGoals(), match.homeGoals());
            });
        List<Row> ordered = rows.values().stream().sorted(
            Comparator.comparingInt((Row r) -> r.points).reversed()
                .thenComparing(Comparator.comparingInt((Row r) -> r.goalsFor - r.goalsAgainst).reversed())
                .thenComparing(Comparator.comparingInt((Row r) -> r.goalsFor).reversed())
                .thenComparing(r -> r.team.name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> r.team.id())
        ).toList();
        List<Standing> result = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Row row = ordered.get(i);
            List<String> form = List.copyOf(row.form.subList(Math.max(0, row.form.size() - 5), row.form.size()));
            result.add(new Standing(i + 1, row.team.id(), row.team.name(), row.team.shortName(), row.team.color(),
                row.played, row.won, row.drawn, row.lost, row.goalsFor, row.goalsAgainst, row.goalsFor - row.goalsAgainst, row.points, form));
        }
        return List.copyOf(result);
    }
}
