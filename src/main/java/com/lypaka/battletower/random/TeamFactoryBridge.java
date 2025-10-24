package com.lypaka.battletower.random;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;

import java.util.ArrayList;
import java.util.List;

public class TeamFactoryBridge {

    /** Constrói a equipe a partir de spec strings (ex.: "pikachu lvl:25 moves:..."). */
    public List<Pokemon> buildFromSpecs(List<String> specs) {
        List<Pokemon> team = new ArrayList<>();
        for (String specStr : specs) {
            try {
                PokemonSpec spec = PokemonSpec.from(specStr);
                Pokemon p = spec.create();
                if (p != null) team.add(p);
            } catch (Throwable t) {
                // logue se quiser
            }
        }
        return team;
    }
}
