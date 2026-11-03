package com.tribuna.analysis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CsvAnalysisServiceTest {
    private final CsvAnalysisService service = new CsvAnalysisService();
    private AnalysisReport analyze(String csv) { return service.analyze(csv.getBytes(StandardCharsets.UTF_8), "test.csv", false); }

    @Test void computesAllFiveRequiredResults() {
        var report = analyze("Nombre;Edad;Equipo;Estado Civil;Nivel de Estudios\nJuan;40;Racing;Casado;Universitario\nAna;20;Racing Club;Soltero;Secundario\nJuan;25;River;Casado;Universitario\nJuan;35;River Plate;Casado;Universitario\nPedro;50;Boca;Soltero;Terciario\n");
        assertThat(report.totalMembers()).isEqualTo(5);
        assertThat(report.teamCount()).isEqualTo(3);
        assertThat(report.racing().averageAge()).isEqualTo(30.0);
        assertThat(report.marriedUniversity()).extracting(AnalysisReport.Member::age).containsExactly(25,35,40);
        assertThat(report.riverNames().getFirst().name()).isEqualTo("Juan");
        assertThat(report.riverNames().getFirst().count()).isEqualTo(2);
        assertThat(report.teams().getFirst().members()).isEqualTo(2);
        assertThat(report.teams().stream().filter(t -> t.team().equals("Racing")).findFirst().orElseThrow().youngestAge()).isEqualTo(20);
    }
    @Test void limitsMarriedUniversityMembersAfterSorting() {
        StringBuilder csv = new StringBuilder();
        for (int i = 119; i >= 0; i--) csv.append("Socio ").append(i).append(';').append(i).append(";River;Casado;Universitario\n");
        var report = analyze(csv.toString());
        assertThat(report.marriedUniversityTotal()).isEqualTo(120);
        assertThat(report.marriedUniversity()).hasSize(100);
        assertThat(report.marriedUniversity().getFirst().age()).isZero();
        assertThat(report.marriedUniversity().getLast().age()).isEqualTo(99);
    }
    @Test void supportsBomQuotesAndAccents() {
        var report = analyze("\uFEFFNombre;Edad;Equipo;Estado Civil;Nivel de Estudios\n\"Ana; María\";25;Huracán;Casada;Universitaria\n");
        assertThat(report.marriedUniversity().getFirst().name()).isEqualTo("Ana; María");
        assertThat(report.teams().getFirst().team()).isEqualTo("Huracán");
    }
    @Test void supportsWindows1252() {
        var report = service.analyze("José;31;River;Casado;Universitario".getBytes(Charset.forName("windows-1252")), "socios.csv", false);
        assertThat(report.riverNames().getFirst().name()).isEqualTo("José");
    }
    @Test void absentTeamsHaveNoInventedAverageOrNames() {
        var report = analyze("Ana;28;Boca;Soltero;Terciario\n");
        assertThat(report.racing().total()).isZero();
        assertThat(report.racing().averageAge()).isNull();
        assertThat(report.riverNames()).isEmpty();
        assertThat(report.marriedUniversity()).isEmpty();
    }
    @Test void failsTheWholeImportOnAnInvalidRow() {
        assertThatThrownBy(() -> analyze("Juan;24;River;Casado;Universitario\nAna;abc;Boca;Soltero;Secundario"))
            .isInstanceOf(InvalidCsvException.class).hasMessageContaining("Fila 2");
    }
    @Test void rejectsEmptyFilesAndHeaderOnly() {
        assertThatThrownBy(() -> analyze("")).isInstanceOf(InvalidCsvException.class);
        assertThatThrownBy(() -> analyze("Nombre;Edad;Equipo;Estado Civil;Nivel de Estudios\n")).isInstanceOf(InvalidCsvException.class);
    }
    @Test void rejectsWrongDelimitersAndOutOfRangeAges() {
        assertThatThrownBy(() -> analyze("Ana,30,Boca,Soltero,Secundario")).hasMessageContaining("5 columnas");
        assertThatThrownBy(() -> analyze("Ana;121;Boca;Soltero;Secundario")).hasMessageContaining("0 y 120");
        assertThatThrownBy(() -> analyze(";20;Boca;Soltero;Secundario")).hasMessageContaining("nombre");
    }
    @Test void riverNamesAreLimitedToFiveWithDeterministicTies() {
        var report = analyze("Felipe;21;River;Soltero;Primario\nEva;22;River;Soltero;Primario\nDani;23;River;Soltero;Primario\nClara;24;River;Soltero;Primario\nBeto;25;River;Soltero;Primario\nAna;26;River;Soltero;Primario");
        assertThat(report.riverNames()).extracting(AnalysisReport.NameCount::name).containsExactly("Ana","Beto","Clara","Dani","Eva");
    }
    @Test void ageBucketsCoverEveryMemberIncludingBoundaries() {
        var report = analyze("Ana;0;Boca;Soltero;Primario\nBeto;18;Boca;Soltero;Primario\nClara;30;Boca;Soltero;Primario\nDani;45;Boca;Soltero;Primario\nEva;120;Boca;Soltero;Primario");
        assertThat(report.ages()).extracting(AnalysisReport.AgeBucket::count).containsExactly(1L,1L,1L,1L,1L);
    }
}
