package com.lypaka.battletower.random;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RandomTeamApplier {

       public static Logger logger = LogManager.getLogger("Battle Tower");

    private final RandomPoolService pools;
    private final TeamRandomizer randomizer;
    private final TeamFactoryBridge bridge;

    public RandomTeamApplier(RandomPoolService pools, TeamRandomizer randomizer, TeamFactoryBridge bridge) {
        this.pools = pools;
        this.randomizer = randomizer;
        this.bridge = bridge;
    }

    /** Injeta um time aleatório no NPC caso haja pool configurado; se não houver, mantém o time original. */
    public void applyIfAvailable(NPCTrainer npc, int tier, String trainerKey, int defaultTeamSize) {
        if (npc == null || !pools.isEnabled()) return;

        List<String> pool = pools.getSpecs(tier, trainerKey);
        if (pool.isEmpty()) return; // sem pool => mantém o que estava no NPC Editor

        int teamSize = pools.getTeamSizeForTier(tier, defaultTeamSize);
        List<String> pickedSpecs = randomizer.pickTeam(pool, teamSize, true);
        List<Pokemon> team = bridge.buildFromSpecs(pickedSpecs);

        logger.info("[BT-Random] pool specs=" + pickedSpecs);
logger.info("[BT-Random] pokemons construidos=" + team.size());


        if (team.isEmpty()) return;

        boolean ok = applyToTrainerParty(npc, team);
        // Se quiser logar:
        // System.out.println("[BT-RandomTeam] Aplicacao no NPC " + npc.getName() + " => " + ok + " | size=" + team.size());
    }

    /** --- Parte que resolve as diferenças de API usando reflection --- */
    private boolean applyToTrainerParty(NPCTrainer npc, List<Pokemon> team) {
        
        try {
            // 1) Tentar via getPokemonStorage()
            Object storage = invokeIfExists(npc, "getPokemonStorage");
            logger.info("[BT-Random] tentando aplicar time via storage=" + storage.getClass().getName());
            if (storage != null) {
                if (tryStorageSet(storage, team)) return true;

                // Fallback: se storage expõe getTeam() como List<Pokemon>
                Object list = invokeIfExists(storage, "getTeam");
                if (list instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Pokemon> party = (List<Pokemon>) list;
                    party.clear();
                    party.addAll(team);
                    markDirtyIfExists(storage);
                    return true;
                }
            }

            // 2) Alguns builds guardam uma List/Pokemon[] como campo dentro do NPC
            // Tentar campos 'party', 'pokemon', 'pokemons'...
            if (tryWriteKnownFields(npc, team)) return true;

            // 3) Fallback duro: procurar qualquer List<Pokemon> em campos do storage e preencher
            if (storage != null && tryWriteAnyPokemonListField(storage, team)) return true;

        } catch (Throwable t) {
            // System.out.println("[BT-RandomTeam] Falha ao aplicar time: " + t);
        }
        return false;
    }

    private boolean tryStorageSet(Object storage, List<Pokemon> team) {
        // Prioridade: set(int,Pokemon)
        Method set = findMethod(storage.getClass(), "set", int.class, Pokemon.class);
        if (set == null) {
            // variantes comuns
            set = findMethod(storage.getClass(), "setPokemon", int.class, Pokemon.class);
            if (set == null) set = findMethod(storage.getClass(), "setSlot", int.class, Pokemon.class);
        }
        if (set != null) {
            // Tentar limpar antes, se houver 'clear()'
            Method clear = findMethod(storage.getClass(), "clear");
            if (clear != null) {
                try { clear.invoke(storage); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < team.size(); i++) {
                try { set.invoke(storage, i, team.get(i)); } catch (Throwable ignored) {}
            }
            markDirtyIfExists(storage);
            return true;
        }
        return false;
    }

    private boolean tryWriteKnownFields(Object target, List<Pokemon> team) {
        // Procura campos com nomes típicos
        String[] names = new String[] { "party", "pokemon", "pokemons", "team" };
        for (String n : names) {
            Field f = findField(target.getClass(), n);
            if (f == null) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(target);
                if (v instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Pokemon> list = (List<Pokemon>) v;
                    list.clear();
                    list.addAll(team);
                    markDirtyIfExists(target);
                    return true;
                } else if (v != null && v.getClass().isArray()
                        && Pokemon.class.isAssignableFrom(v.getClass().getComponentType())) {
                    int len = Array.getLength(v);
                    for (int i = 0; i < Math.min(len, team.size()); i++) {
                        Array.set(v, i, team.get(i));
                    }
                    markDirtyIfExists(target);
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private boolean tryWriteAnyPokemonListField(Object target, List<Pokemon> team) {
        for (Field f : target.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object v = f.get(target);
                if (v instanceof List) {
                    List<?> raw = (List<?>) v;
                    // Checar se é List<Pokemon> (pelo menos por conteúdo)
                    if (!raw.isEmpty() && !(raw.get(0) instanceof Pokemon)) continue;

                    @SuppressWarnings("unchecked")
                    List<Pokemon> list = (List<Pokemon>) raw;
                    list.clear();
                    list.addAll(team);
                    markDirtyIfExists(target);
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private void markDirtyIfExists(Object storage) {
        Method dirty = findMethod(storage.getClass(), "markDirty");
        if (dirty == null) dirty = findMethod(storage.getClass(), "setDirty");
        if (dirty != null) {
            try { dirty.invoke(storage); } catch (Throwable ignored) {}
        }
    }

    /* ----------------- util reflection ----------------- */

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); } catch (NoSuchMethodException ignored) {}
        try { return c.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
        return null;
    }

    private static Field findField(Class<?> c, String name) {
        try { return c.getField(name); } catch (NoSuchFieldException ignored) {}
        try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        return null;
    }

    private static Object invokeIfExists(Object target, String method, Object... args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = (args[i] == null) ? Object.class : args[i].getClass();

        // Tenta pública
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(method)) continue;
            try {
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (IllegalArgumentException ignored) {
                // assinatura não bate; tenta próxima
            } catch (Throwable t) {
                return null;
            }
        }
        // Tenta declarada
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (!m.getName().equals(method)) continue;
            try {
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (IllegalArgumentException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }
}
