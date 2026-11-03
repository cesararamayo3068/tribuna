package com.tribuna.analysis;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class AnalysisRepository {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    public AnalysisRepository(JdbcTemplate jdbc, JsonMapper json) { this.jdbc = jdbc; this.json = json; }
    public void save(AnalysisReport report) {
        jdbc.update("INSERT INTO analyses (id, file_name, created_at, member_count, report) VALUES (?, ?, ?, ?, ?)",
            report.id(), report.fileName(), Timestamp.from(report.createdAt()), report.totalMembers(), json.writeValueAsString(report));
    }
    public List<AnalysisReport.HistoryItem> recent() {
        return jdbc.query("SELECT id, file_name, created_at, member_count FROM analyses ORDER BY created_at DESC LIMIT 20",
            (rs, row) -> new AnalysisReport.HistoryItem(rs.getObject("id", UUID.class), rs.getString("file_name"), rs.getTimestamp("created_at").toInstant(), rs.getInt("member_count")));
    }
    public Optional<AnalysisReport> find(UUID id) {
        return jdbc.query("SELECT report FROM analyses WHERE id = ?", (rs, row) -> json.readValue(rs.getString("report"), AnalysisReport.class), id).stream().findFirst();
    }
    public boolean healthy() { return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class)); }
}
