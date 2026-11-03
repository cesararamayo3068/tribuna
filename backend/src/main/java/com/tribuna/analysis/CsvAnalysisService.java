package com.tribuna.analysis;

import static com.tribuna.analysis.AnalysisReport.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
public class CsvAnalysisService {
    private static final int MAX_ROWS = 100_000;
    private static final List<String> HEADERS = List.of("nombre", "edad", "equipo", "estadocivil", "niveldeestudios");

    public AnalysisReport analyze(byte[] bytes, String fileName, boolean demo) {
        if (bytes.length == 0) throw new InvalidCsvException("El archivo está vacío. Cargá un CSV con al menos un socio.");
        if (bytes.length > 5 * 1024 * 1024) throw new InvalidCsvException("El archivo supera el límite de 5 MB.");
        String text = decode(bytes).replaceFirst("^\\uFEFF", "");
        List<Member> members = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).setIgnoreEmptyLines(true).get();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            boolean first = true;
            for (CSVRecord row : parser) {
                long line = row.getRecordNumber();
                if (row.size() != 5) throw new InvalidCsvException("Fila " + line + ": se esperaban 5 columnas separadas por punto y coma (;).");
                if (first && key(row.get(0)).equals("nombre") && key(row.get(1)).equals("edad")) {
                    for (int i = 0; i < 5; i++) {
                        if (!key(row.get(i)).equals(HEADERS.get(i))) throw new InvalidCsvException("El encabezado debe ser Nombre;Edad;Equipo;Estado Civil;Nivel de Estudios.");
                    }
                    first = false;
                    continue;
                }
                first = false;
                if (members.size() >= MAX_ROWS) throw new InvalidCsvException("El límite es de 100.000 socios por archivo.");
                String name = required(row.get(0), "nombre", line);
                int age;
                try { age = Integer.parseInt(row.get(1)); }
                catch (NumberFormatException e) { throw new InvalidCsvException("Fila " + line + ": la edad debe ser un número entero."); }
                if (age < 0 || age > 120) throw new InvalidCsvException("Fila " + line + ": la edad debe estar entre 0 y 120 años.");
                String team = canonicalTeam(required(row.get(2), "equipo", line));
                String status = required(row.get(3), "estado civil", line);
                String education = required(row.get(4), "nivel de estudios", line);
                if (key(status).startsWith("casad")) status = "Casado";
                if (Set.of("universitario", "universitaria", "universitarios", "universitarias").contains(key(education))) education = "Universitario";
                members.add(new Member(name, age, team, status, education));
            }
        } catch (InvalidCsvException e) { throw e; }
        catch (IOException | RuntimeException e) { throw new InvalidCsvException("No se pudo leer el CSV. Revisá las comillas y los separadores de las filas."); }
        if (members.isEmpty()) throw new InvalidCsvException("El archivo no contiene socios. Agregá al menos una fila de datos.");
        return summarize(members, fileName, demo);
    }

    private AnalysisReport summarize(List<Member> members, String fileName, boolean demo) {
        var byTeam = members.stream().collect(Collectors.groupingBy(Member::team));
        var teams = byTeam.entrySet().stream().map(entry -> {
            var stats = entry.getValue().stream().mapToInt(Member::age).summaryStatistics();
            return new TeamStats(entry.getKey(), stats.getCount(), round(stats.getAverage()), stats.getMin(), stats.getMax());
        }).sorted(Comparator.comparingLong(TeamStats::members).reversed().thenComparing(TeamStats::team)).toList();
        var racing = members.stream().filter(m -> m.team().equals("Racing")).mapToInt(Member::age).summaryStatistics();
        var married = members.stream().filter(m -> key(m.maritalStatus()).equals("casado") && key(m.education()).equals("universitario"))
            .sorted(Comparator.comparingInt(Member::age).thenComparing(Member::name).thenComparing(Member::team)).toList();
        var river = members.stream().filter(m -> m.team().equals("River")).collect(Collectors.groupingBy(m -> key(m.name())));
        var riverNames = river.values().stream().map(list -> new NameCount(list.getFirst().name(), list.size()))
            .sorted(Comparator.comparingLong(NameCount::count).reversed().thenComparing(NameCount::name)).limit(5).toList();
        int[] boundaries = {0, 18, 30, 45, 60, 121};
        String[] labels = {"0–17", "18–29", "30–44", "45–59", "60+"};
        List<AgeBucket> ages = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            int lower = boundaries[i], upper = boundaries[i + 1];
            ages.add(new AgeBucket(labels[i], members.stream().filter(m -> m.age() >= lower && m.age() < upper).count()));
        }
        var education = members.stream().map(Member::education).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream().map(e -> new EducationCount(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(EducationCount::count).reversed().thenComparing(EducationCount::label)).toList();
        var stats = members.stream().mapToInt(Member::age).summaryStatistics();
        return new AnalysisReport(UUID.randomUUID(), fileName, Instant.now(), demo, members.size(), byTeam.size(), round(stats.getAverage()),
            stats.getMin(), stats.getMax(), new RacingStats((int) racing.getCount(), racing.getCount() == 0 ? null : round(racing.getAverage())),
            married.stream().limit(100).toList(), married.size(), riverNames, teams, ages, education);
    }

    private static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) { return Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString(); }
    }
    private static String required(String value, String field, long row) {
        String clean = value.strip().replaceAll("\\s+", " ");
        if (clean.isEmpty()) throw new InvalidCsvException("Fila " + row + ": falta el campo " + field + ".");
        if (clean.length() > 150) throw new InvalidCsvException("Fila " + row + ": el campo " + field + " supera los 150 caracteres.");
        return clean;
    }
    static String key(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static String canonicalTeam(String team) {
        return switch (key(team)) {
            case "racing", "racingclub", "racingclubdeavellaneda" -> "Racing";
            case "river", "riverplate", "clubatleticoriverplate" -> "River";
            case "boca", "bocajuniors" -> "Boca";
            case "independiente" -> "Independiente";
            case "sanlorenzo" -> "San Lorenzo";
            case "newells", "newellsoldboys" -> "Newell’s";
            case "huracan" -> "Huracán";
            case "velez", "velezsarsfield" -> "Vélez";
            case "estudiantes", "estudiantesdelaplata" -> "Estudiantes";
            case "gimnasia", "gimnasialp", "gimnasiay esgrimalaplata", "gimnasiayesgrimalaplata" -> "Gimnasia LP";
            case "rosariocentral" -> "Rosario Central";
            default -> team;
        };
    }
    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
}
