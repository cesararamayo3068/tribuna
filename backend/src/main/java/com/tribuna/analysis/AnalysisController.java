package com.tribuna.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AnalysisController {
    private final CsvAnalysisService service;
    private final AnalysisRepository repository;
    private final boolean demo;
    public AnalysisController(CsvAnalysisService service, AnalysisRepository repository, Environment environment) {
        this.service = service;
        this.repository = repository;
        this.demo = environment.acceptsProfiles(Profiles.of("demo"));
    }
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean healthy = repository.healthy();
        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("status", healthy ? "UP" : "DOWN", "demo", demo));
    }
    @GetMapping("/analyses/demo")
    public AnalysisReport demo() throws IOException {
        return service.analyze(new ClassPathResource("socios-demo.csv").getContentAsByteArray(), "socios-demo.csv", true);
    }
    @GetMapping(value = "/sample.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> sample() throws IOException {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"socios-demo.csv\"")
            .body(new ClassPathResource("socios-demo.csv").getContentAsByteArray());
    }
    @PostMapping(value = "/analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisReport upload(@RequestPart("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "socios.csv" : file.getOriginalFilename().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) throw new InvalidCsvException("Elegí un archivo con extensión .csv.");
        if (name.length() > 255) throw new InvalidCsvException("El nombre del archivo es demasiado largo.");
        AnalysisReport report = service.analyze(file.getBytes(), name, false);
        repository.save(report);
        return report;
    }
    @GetMapping("/analyses")
    public List<AnalysisReport.HistoryItem> recent() { return repository.recent(); }
    @GetMapping("/analyses/{id}")
    public AnalysisReport find(@PathVariable UUID id) {
        return repository.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese análisis."));
    }
}
