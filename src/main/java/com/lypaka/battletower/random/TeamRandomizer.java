package com.lypaka.battletower.random;

import java.util.*;

public class TeamRandomizer {

    private final Random rng = new Random();

    public List<String> pickTeam(List<String> pool, int teamSize, boolean allowDuplicatesWhenSmall) {
        if (pool == null || pool.isEmpty() || teamSize <= 0) return Collections.emptyList();

        List<String> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, rng);

        if (copy.size() >= teamSize) {
            return new ArrayList<>(copy.subList(0, teamSize));
        }

        if (!allowDuplicatesWhenSmall) {
            return copy; // time menor
        }

        // completa com reposição
        List<String> result = new ArrayList<>(copy);
        while (result.size() < teamSize) {
            result.add(pool.get(rng.nextInt(pool.size())));
        }
        return result;
    }
}
