package com.lypaka.battletower.random;

import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class RandomPoolService {

    private final ConfigurationNode root;

    public RandomPoolService(CommentedConfigurationNode randomRoot) {
        this.root = randomRoot;
    }

    public boolean isEnabled() {
        return root.getNode("enabled").getBoolean(true);
    }

    public int getTeamSizeForTier(int tier, int fallback) {
        return root.getNode("tiers", String.valueOf(tier), "teamSize").getInt(fallback);
    }

    /** Retorna uma lista de spec strings para (tier, trainerKey). */
    public List<String> getSpecs(int tier, String trainerKey) {
        List<String> result = new ArrayList<>();

        ConfigurationNode tierNode = root.getNode("tiers", String.valueOf(tier));
        if (tierNode.isVirtual()) return Collections.emptyList();

        // 1) pool específico do treinador
        if (trainerKey != null && !trainerKey.isEmpty()) {
            List<String> trainerPool = readStringList(
                tierNode.getNode("trainers", trainerKey)
            );
            if (!trainerPool.isEmpty()) result.addAll(trainerPool);
        }

        // 2) pool padrão do tier
        result.addAll(readStringList(tierNode.getNode("defaultPool")));

        return result;
    }

    private List<String> readStringList(ConfigurationNode node) {
        if (node == null || node.isVirtual() || !node.hasListChildren()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (ConfigurationNode n : node.getChildrenList()) {
            String s = n.getString();
            if (s != null && !s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }
}
