package com.tribuna.tournament;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TournamentModels {
    private TournamentModels() {}
    public record Summary(UUID id, String name, String season, String location, Instant createdAt,
        int teamCount, int totalMatches, int playedMatches, boolean fixtureGenerated) {}
    public record Team(UUID id, UUID tournamentId, String name, String shortName, String color) {}
    public record Match(UUID id, int round, UUID homeId, String homeName, String homeShortName, String homeColor,
        UUID awayId, String awayName, String awayShortName, String awayColor, Integer homeGoals, Integer awayGoals,
        Instant kickoffAt, String field, long version) {
        public boolean played() { return homeGoals != null && awayGoals != null; }
    }
    public record Standing(int position, UUID teamId, String name, String shortName, String color,
        int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst, int goalDifference, int points, List<String> form) {}
    public record Detail(Summary tournament, List<Team> teams, List<Match> matches, List<Standing> standings) {}
    public record CreateTournament(String name, String season, String location) {}
    public record CreateTeam(String name, String shortName, String color) {}
    public record ResultRequest(java.math.BigDecimal homeGoals, java.math.BigDecimal awayGoals, Long version) {}
    public record ScheduleRequest(String kickoffAt, String field, Long version) {}
    public record Pairing(int round, UUID homeId, UUID awayId) {}
}
