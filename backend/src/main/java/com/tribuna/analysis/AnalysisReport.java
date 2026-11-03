package com.tribuna.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisReport(
    UUID id, String fileName, Instant createdAt, boolean demo,
    int totalMembers, int teamCount, double averageAge, int youngestAge, int oldestAge,
    RacingStats racing, List<Member> marriedUniversity, int marriedUniversityTotal,
    List<NameCount> riverNames, List<TeamStats> teams, List<AgeBucket> ages,
    List<EducationCount> education
) {
    public record Member(String name, int age, String team, String maritalStatus, String education) {}
    public record RacingStats(int total, Double averageAge) {}
    public record NameCount(String name, long count) {}
    public record TeamStats(String team, long members, double averageAge, int youngestAge, int oldestAge) {}
    public record AgeBucket(String label, long count) {}
    public record EducationCount(String label, long count) {}
    public record HistoryItem(UUID id, String fileName, Instant createdAt, int totalMembers) {}
}
