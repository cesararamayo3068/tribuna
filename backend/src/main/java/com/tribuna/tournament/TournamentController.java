package com.tribuna.tournament;

import static com.tribuna.tournament.TournamentModels.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final TournamentService service;
    public TournamentController(TournamentService service) { this.service=service; }
    @GetMapping public List<Summary> list() { return service.list(); }
    @GetMapping("/{id}") public Detail detail(@PathVariable UUID id) { return service.detail(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@RequestBody CreateTournament input) { return service.create(input); }
    @PostMapping("/{id}/teams") @ResponseStatus(HttpStatus.CREATED)
    public Detail addTeam(@PathVariable UUID id,@RequestBody CreateTeam input) { return service.addTeam(id,input); }
    @DeleteMapping("/{id}/teams/{teamId}")
    public Detail removeTeam(@PathVariable UUID id,@PathVariable UUID teamId) { return service.removeTeam(id,teamId); }
    @PostMapping("/{id}/fixture") @ResponseStatus(HttpStatus.CREATED)
    public Detail generate(@PathVariable UUID id) { return service.generateFixture(id); }
    @PutMapping("/{id}/matches/{matchId}/result")
    public Detail result(@PathVariable UUID id,@PathVariable UUID matchId,@RequestBody ResultRequest input) { return service.result(id,matchId,input); }
    @PutMapping("/{id}/matches/{matchId}/schedule")
    public Detail schedule(@PathVariable UUID id,@PathVariable UUID matchId,@RequestBody ScheduleRequest input) { return service.schedule(id,matchId,input); }
}
