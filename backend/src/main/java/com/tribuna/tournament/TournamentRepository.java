package com.tribuna.tournament;

import static com.tribuna.tournament.TournamentModels.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TournamentRepository {
    private final JdbcTemplate jdbc;
    public TournamentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String SUMMARY_SQL = """
        SELECT t.*,
          (SELECT COUNT(*) FROM tournament_teams tt WHERE tt.tournament_id=t.id) AS team_count,
          (SELECT COUNT(*) FROM tournament_matches tm WHERE tm.tournament_id=t.id) AS match_count,
          (SELECT COUNT(*) FROM tournament_matches tm WHERE tm.tournament_id=t.id AND tm.home_goals IS NOT NULL) AS played_count
        FROM tournaments t
        """;
    private final RowMapper<Summary> summaryMapper = (rs, index) -> new Summary(
        rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("season"), rs.getString("location"),
        rs.getTimestamp("created_at").toInstant(), rs.getInt("team_count"), rs.getInt("match_count"),
        rs.getInt("played_count"), rs.getInt("match_count") > 0);
    private final RowMapper<Team> teamMapper = (rs, index) -> new Team(
        rs.getObject("id", UUID.class), rs.getObject("tournament_id", UUID.class),
        rs.getString("name"), rs.getString("short_name"), rs.getString("color"));
    private final RowMapper<Match> matchMapper = (rs, index) -> new Match(
        rs.getObject("id", UUID.class), rs.getInt("round_number"),
        rs.getObject("home_id", UUID.class), rs.getString("home_name"), rs.getString("home_short"), rs.getString("home_color"),
        rs.getObject("away_id", UUID.class), rs.getString("away_name"), rs.getString("away_short"), rs.getString("away_color"),
        rs.getObject("home_goals", Integer.class), rs.getObject("away_goals", Integer.class),
        rs.getTimestamp("kickoff_at") == null ? null : rs.getTimestamp("kickoff_at").toInstant(),
        rs.getString("field"), rs.getLong("version"));
    private static final String MATCH_SQL = """
        SELECT m.*, h.name AS home_name, h.short_name AS home_short, h.color AS home_color,
          a.name AS away_name, a.short_name AS away_short, a.color AS away_color
        FROM tournament_matches m JOIN tournament_teams h ON m.home_id=h.id JOIN tournament_teams a ON m.away_id=a.id
        """;
    public List<Summary> list() { return jdbc.query(SUMMARY_SQL + " ORDER BY t.created_at DESC, t.id", summaryMapper); }
    public Optional<Summary> summary(UUID id) {
        return jdbc.query(SUMMARY_SQL + " WHERE t.id=?", summaryMapper, id).stream().findFirst();
    }
    public boolean lock(UUID id) {
        return !jdbc.query("SELECT id FROM tournaments WHERE id=? FOR UPDATE", (rs,i) -> rs.getObject(1,UUID.class), id).isEmpty();
    }
    public void lockScheduling() { jdbc.query("SELECT pg_advisory_xact_lock(813042)", rs -> { while (rs.next()) {} return null; }); }
    public void lockSeeding() { jdbc.query("SELECT pg_advisory_xact_lock(813043)", rs -> { while (rs.next()) {} return null; }); }
    public void insertTournament(UUID id, String name, String season, String location) {
        jdbc.update("INSERT INTO tournaments(id,name,season,location) VALUES (?,?,?,?)", id,name,season,location);
    }
    public List<Team> teams(UUID id) {
        return jdbc.query("SELECT * FROM tournament_teams WHERE tournament_id=? ORDER BY created_at,id", teamMapper,id);
    }
    public boolean duplicateName(UUID id, String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tournament_teams WHERE tournament_id=? AND name_key=?", Integer.class,id,key) > 0;
    }
    public void insertTeam(UUID id, UUID tournament, String name, String key, String shortName, String color) {
        jdbc.update("INSERT INTO tournament_teams(id,tournament_id,name,name_key,short_name,color) VALUES (?,?,?,?,?,?)",id,tournament,name,key,shortName,color);
    }
    public int deleteTeam(UUID tournament, UUID team) {
        return jdbc.update("DELETE FROM tournament_teams WHERE tournament_id=? AND id=?", tournament,team);
    }
    public List<Match> matches(UUID tournament) {
        return jdbc.query(MATCH_SQL + " WHERE m.tournament_id=? ORDER BY m.round_number,h.name,m.id",matchMapper,tournament);
    }
    public Optional<Match> match(UUID tournament, UUID match) {
        return jdbc.query(MATCH_SQL + " WHERE m.tournament_id=? AND m.id=?",matchMapper,tournament,match).stream().findFirst();
    }
    public void insertMatch(UUID tournament, Pairing pair) {
        jdbc.update("INSERT INTO tournament_matches(id,tournament_id,round_number,home_id,away_id) VALUES (?,?,?,?,?)",
            UUID.randomUUID(),tournament,pair.round(),pair.homeId(),pair.awayId());
    }
    public int updateResult(UUID tournament, UUID id, Integer home, Integer away, long version) {
        return jdbc.update("UPDATE tournament_matches SET home_goals=?,away_goals=?,version=version+1 WHERE tournament_id=? AND id=? AND version=?",
            home,away,tournament,id,version);
    }
    public boolean scheduleConflict(Match match, Instant kickoff, String fieldKey) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM tournament_matches WHERE id<>? AND kickoff_at IS NOT NULL
            AND ABS(EXTRACT(EPOCH FROM (kickoff_at - CAST(? AS timestamptz)))) < 5400
            AND (field_key=? OR home_id IN (?,?) OR away_id IN (?,?))
            """, Integer.class, match.id(),Timestamp.from(kickoff),fieldKey,match.homeId(),match.awayId(),match.homeId(),match.awayId()) > 0;
    }
    public int updateSchedule(UUID tournament, UUID id, Instant kickoff, String field, String key, long version) {
        return jdbc.update("UPDATE tournament_matches SET kickoff_at=?,field=?,field_key=?,version=version+1 WHERE tournament_id=? AND id=? AND version=?",
            kickoff == null ? null : Timestamp.from(kickoff),field,key,tournament,id,version);
    }
}
