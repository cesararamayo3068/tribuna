package com.tribuna.tournament;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="tribuna.demo.enabled",havingValue="true",matchIfMissing=true)
public class DemoTournamentInitializer implements ApplicationRunner {
    private final TournamentService service;
    public DemoTournamentInitializer(TournamentService service) { this.service=service; }
    @Override public void run(ApplicationArguments args) { service.seedDemo(); }
}
