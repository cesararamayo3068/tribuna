package com.tribuna.tournament;

import java.util.*;
import static com.tribuna.tournament.TournamentModels.Pairing;

public final class RoundRobin {
    private RoundRobin() {}
    public static List<Pairing> generate(List<UUID> teams) {
        if (teams.size() < 2 || teams.size() > 20 || new HashSet<>(teams).size() != teams.size() || teams.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("El torneo necesita entre 2 y 20 equipos distintos.");
        List<UUID> rotation = new ArrayList<>(teams);
        if (rotation.size() % 2 != 0) rotation.add(null);
        List<Pairing> matches = new ArrayList<>();
        int size = rotation.size();
        for (int round = 1; round < size; round++) {
            for (int i = 0; i < size / 2; i++) {
                UUID a = rotation.get(i), b = rotation.get(size - 1 - i);
                if (a == null || b == null) continue;
                matches.add((round + i) % 2 == 0 ? new Pairing(round, b, a) : new Pairing(round, a, b));
            }
            rotation.add(1, rotation.removeLast());
        }
        return List.copyOf(matches);
    }
}
