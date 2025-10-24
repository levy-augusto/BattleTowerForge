package com.lypaka.battletower.Config;

import com.google.common.reflect.TypeToken;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigGetters {

    /* -------------------- cache opcional -------------------- */
    private static final Set<String> BATTLE_TOWER_NPC_LOCATIONS = new HashSet<>();

    /* -------------------- player state -------------------- */
    public static int getCurrentBP(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(), "BP-Amount").getInt();
    }

    public static ConfigurationNode getBoosterNode() {
    // bp-shop.conf (index 1) → Boosters.Modifier
    return ConfigManager.getConfigNode(1, "Boosters", "Modifier");
}

    public static void setBP(EntityPlayer player, int amount) {
        ConfigManager.getPlayerConfigNode(player.getUniqueID(), "BP-Amount").setValue(amount);
    }

    public static int getCurrentTier(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                "Current-Challenge-Info", "Current-Tier").getInt();
    }

    public static int getCurrentTrainer(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                "Current-Challenge-Info", "Current-Trainer").getInt();
    }

    public static String getCurrentMode(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                "Current-Challenge-Info", "Battle-Mode").getString();
    }

    public static boolean isInTower(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                "Current-Challenge-Info", "Is-Challenging").getBoolean();
    }

    public static boolean isChallenging(EntityPlayer player) {
        return ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                "Current-Challenge-Info", "Is-Challenging").getBoolean(false);
    }

    /* -------------------- global settings -------------------- */
    /** battle-rooms.conf (idx 0) → Battle-Tiers.Amount-Of-Tiers */
    public static int getTierAmount() {
        int v = ConfigManager.getConfigNode(0, "Battle-Tiers", "Amount-Of-Tiers").getInt(0);
        if (v > 0) return v;

        // fallback: conta quantos "Tier-n" existem em Battle-Rooms/Locations
        int count = 0;
        for (int i = 1; i <= 64; i++) {
            ConfigurationNode n = ConfigManager.getConfigNode(0, "Battle-Rooms", "Locations", "Tier-" + i);
            if (n.isVirtual()) break;
            count++;
        }
        return count;
    }

    /** legacy; só use se realmente existir no conf. Preferir getRoomCountForTier(tier). */
    public static int getTrainerPerRoundAmount() {
        return ConfigManager.getConfigNode(0, "Battle-Tiers", "Trainers-Per-Tier").getInt(0);
    }

    public static int getMaxTrainerAmount() {
        return getTierAmount() * Math.max(1, getTrainerPerRoundAmount());
    }

    public static int getMaxPartySize() {
        return ConfigManager.getConfigNode(0, "Misc", "Team-Amount").getInt(6);
    }

    public static String getCratesCommand() {
        return ConfigManager.getConfigNode(1, "Settings", "Crate-Key-Command").getString("");
    }

    public static List<String> getPokemonBlacklist() throws ObjectMappingException {
        return ConfigManager.getConfigNode(0, "Misc", "Pokemon-Blacklist")
                .getList(TypeToken.of(String.class), Collections.emptyList());
    }

    public static int getMinOpenLevel() {
        return ConfigManager.getConfigNode(0, "Misc", "Open-Level-Min").getInt(0);
    }

    public static List<Integer> getBPShops() throws ObjectMappingException {
        return ConfigManager.getConfigNode(1, "Settings", "Shop-List")
                .getList(TypeToken.of(Integer.class), Collections.emptyList());
    }

    public static boolean useExternalNPCConfigs() {
        return ConfigManager.getConfigNode(0, "Misc", "External-NPC-Configs").getBoolean(false);
    }

    public static int getBPEarned(String trainerID) {
        return ConfigManager.getConfigNode(0, "Trainer-BP", trainerID).getInt(0);
    }

    /* -------------------- rooms & navigation -------------------- */
    /**
     * Pega a localização da sala (Room-n) a partir do tier e “treinador” (índice da room).
     * battle-rooms.conf (idx 0) → Battle-Rooms.Locations.Tier-X.Room-Y
     */
    public static String getRoomLocationFromTier(int tier, int trainerIndex) {
        // chefe final opcional
        if (trainerIndex == getTrainerPerRoundAmount() && tier == getTierAmount()) {
            String boss = ConfigManager.getConfigNode(0, "Battle-Rooms", "Locations", "Head-Boss-Room").getString(null);
            if (boss != null && !boss.trim().isEmpty()) return boss;
        }

        return ConfigManager.getConfigNode(0,
                        "Battle-Rooms", "Locations",
                        "Tier-" + tier, "Room-" + trainerIndex)
                .getString(null);
    }

    /** Conta quantas rooms existem de fato no tier (Room-1..Room-N). */
    public static int getRoomCountForTier(int tier) {
        int count = 0;
        for (int i = 1; i <= 64; i++) {
            String loc = getRoomLocationFromTier(tier, i);
            if (loc == null || loc.trim().isEmpty()) break;
            count++;
        }
        return count;
    }

    /** Atalho compatível: usa getRoomLocationFromTier. */
    public static String getNextRoom(EntityPlayer player) {
        int tier = getCurrentTier(player);
        int trainer = getCurrentTrainer(player);
        return getRoomLocationFromTier(tier, trainer);
    }

    /** World IDs em battle-rooms.conf (idx 0) → World-IDs.<name> */
    public static int getWorldID(String worldName) {
        return ConfigManager.getConfigNode(0, "World-IDs", worldName).getInt(-100);
    }

    /* -------------------- NPCs / Trainers / Reception -------------------- */
    /** npc-settings.conf (idx 2) → NPC-Settings.Trainer-Locations.Tier-X.Trainer-Y */
    public static void setTrainerLocation(int tier, int trainer, String location) throws IOException {
        ConfigManager.getConfigNode(2, "NPC-Settings", "Trainer-Locations",
                "Tier-" + tier, "Trainer-" + trainer).setValue(location);
        ConfigManager.saveAll();
    }

    public static String getTrainerLocation(int tier, int trainer) {
        return ConfigManager.getConfigNode(2, "NPC-Settings", "Trainer-Locations",
                "Tier-" + tier, "Trainer-" + trainer).getString(null);
    }

    /** npc-settings.conf (idx 2) → NPC-Settings.Receptionist-Locations */
    public static List<String> getReceptionistLocations() {
        ConfigurationNode n = ConfigManager.getConfigNode(2, "NPC-Settings", "Receptionist-Locations");
        try {
            List<String> list = n.getList(node -> ((ConfigurationNode) node).getString(), Collections.emptyList());
            return list.stream().filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.toList());
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    /** npc-settings.conf (idx 2) → NPC-Settings.Entrance-Location */
    public static String getEntranceLocation() {
        return ConfigManager.getConfigNode(2, "NPC-Settings", "Entrance-Location").getString(null);
    }

    /* -------------------- cache util opcional -------------------- */
    public static boolean isBattleTowerNPC(String location) {
        return BATTLE_TOWER_NPC_LOCATIONS.contains(location);
    }

    public static void addBattleTowerNPC(String location) {
        BATTLE_TOWER_NPC_LOCATIONS.add(location);
    }

    public static void removeBattleTowerNPC(String location) {
        BATTLE_TOWER_NPC_LOCATIONS.remove(location);
    }

    public static void clearBattleTowerNPCs() {
        BATTLE_TOWER_NPC_LOCATIONS.clear();
    }

    /** Carrega as recepções no cache (usa Receptionist-Locations). */
    public static void loadBattleTowerNPCs() {
        try {
            List<String> list = ConfigManager
                    .getConfigNode(2, "NPC-Settings", "Receptionist-Locations")
                    .getList(TypeToken.of(String.class), Collections.emptyList());
            BATTLE_TOWER_NPC_LOCATIONS.clear();
            BATTLE_TOWER_NPC_LOCATIONS.addAll(list);
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    public static int challengeCooldownMinutes;
        public static void load() {
        challengeCooldownMinutes = ConfigManager
                .getConfigNode(5, "Tower", "challenge-cooldown")
                .getInt(60);
    }

    /* -------------------- edição de rooms (opcional) -------------------- */
    /** battle-rooms.conf (idx 0) → Battle-Rooms.Locations.Tier-X.Room-Y */
    public static void setRoomLocation(int tier, int room, String location) throws IOException {
        ConfigManager.getConfigNode(0, "Battle-Rooms", "Locations",
                "Tier-" + tier, "Room-" + room).setValue(location);
        ConfigManager.saveAll();
    }
}
