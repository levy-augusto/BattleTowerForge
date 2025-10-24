// =============================
// File: src/main/java/com/lypaka/battletower/Config/TierHandler.java
// =============================
package com.lypaka.battletower.Config;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Guis.TeamSelectionMenu;
import com.lypaka.battletower.Handlers.TeleportHandler;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


/** Inicia e controla o desafio de Battle Tower. */
public class TierHandler {

    /* --------------------------------------------------------------------- */
    /*  BOTAO CONFIRM → entra na torre                                      */
    /* --------------------------------------------------------------------- */
    public static void startChallenge(EntityPlayer player,
                                      Map<EntityPlayer, Map<Integer, Pokemon>> teamMap,
                                      String mode)
            throws ObjectMappingException, IOException {

        UIManager.closeUI((EntityPlayerMP) player);

        /* -- cooldown global ---------------------------------------------- */
        String cdRaw = ConfigManager.getConfigNode(0, "Misc", "Cooldown").getString("disabled");
        if (!cdRaw.equalsIgnoreCase("disabled")) {
            String[] p = cdRaw.split(" ");
            int amount = Integer.parseInt(p[0]);
            String unit = p[1].toLowerCase();
            LocalDateTime expiry = LocalDateTime.now();

            switch (unit) {
                case "second": case "seconds": expiry = expiry.plusSeconds(amount); break;
                case "minute": case "minutes": expiry = expiry.plusMinutes(amount); break;
                case "hour"  : case "hours"  : expiry = expiry.plusHours(amount);   break;
                case "day"   : case "days"   : expiry = expiry.plusDays(amount);    break;
                default                     : expiry = expiry.plusWeeks(amount);   break;
            }
            ConfigManager.getPlayerConfigNode(player.getUniqueID(), "Next-Challenge-Time")
                         .setValue(expiry.toString());
        }

        /* -- teleporte para a Room 1 -------------------------------------- */
        String locString = ConfigGetters.getRoomLocationFromTier(1, 1);
        if (locString == null || locString.trim().isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Nenhuma sala definida para Tier 1 / Room 1!"));
            return;
        }

        String[] loc = locString.split(",");
        String world = loc[0].trim();
        BlockPos pos = new BlockPos(
                Integer.parseInt(loc[1].trim()),
                Integer.parseInt(loc[2].trim()),
                Integer.parseInt(loc[3].trim()));
        int dimID = ConfigManager.getWorldID(world);
        if (dimID == -100 && !BattleTower.isSpongeLoaded && !BattleTower.isSpigotLoaded) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Teleportation error. Contact staff."));
            return;
        }
        TeleportHandler.teleportPlayer(player, dimID, world, pos);

        /* -- aplica o time selecionado ------------------------------------ */
        PartyStorage storage = Pixelmon.storageManager.getParty((EntityPlayerMP) player);
        Map<Integer, Pokemon> reserve = teamMap.getOrDefault(player, Collections.emptyMap());

        // Impede continuar se o jogador nao escolheu nada
        if (reserve == null || reserve.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Voce nao selecionou nenhum Pokemon para o desafio!"));
            return;
        }

        // Limpa toda a party
        for (int i = 0; i < 6; i++) storage.set(i, null);

        // Preenche com a selecao (cura 100%)
        for (Map.Entry<Integer, Pokemon> entry : reserve.entrySet()) {
            int slot = entry.getKey();
            Pokemon poke = entry.getValue();
            poke.heal();
            poke.setHealth(poke.getStat(StatsType.HP));
            storage.set(slot, poke);
        }

        if (storage.countPokemon() == 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Erro inesperado: party vazia."));
            return;
        }

        /* -- status da challenge ------------------------------------------ */
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Tier",      1);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Trainer",   1);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Battle-Mode",       mode);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Is-Challenging",    true);
        ConfigManager.savePlayerConfig(player.getUniqueID());

        TeamSelectionMenu.clearSelection(player);
        saveProgress(player);

        // no final do startChallenge(...)
        Sponge.getScheduler().createTaskBuilder()
            .delayTicks(2)
            .execute(() -> placePlayerAtTrainer((EntityPlayerMP) player, 1, 1))
            .submit(pluginOwner());

    }

    /* ----------------------------------------------------------------- */
    /*  Avanca para a proxima sala / trainer                              */
    /* ----------------------------------------------------------------- */
    public static void nextChallenge(EntityPlayer player) throws ObjectMappingException {
        int tier       = ConfigGetters.getCurrentTier(player);
        int trainer    = ConfigGetters.getCurrentTrainer(player);
        int maxTiers   = ConfigGetters.getTierAmount();
        int perTier  = ConfigGetters.getRoomCountForTier(tier);      


        int nextTrainer, nextTier;
        if (trainer < perTier) {
            nextTrainer = trainer + 1;
            nextTier = tier;
        } else {
            nextTrainer = 1;
            nextTier = tier + 1;
        }


        if (nextTier > maxTiers) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "Parabens! Voce concluiu todos os tiers!"));
                    try {
                        TeleportHandler.teleportToEntrance((EntityPlayerMP) player);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        player.sendMessage(new TextComponentString(
                                TextFormatting.YELLOW + "Nao foi possivel te levar para a recepcao automaticamente."));
                    }
                // Reseta o estado do desafio (sem a mensagem de 'Desafio cancelado')
            ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Is-Challenging", false);
            ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Tier",    0);
            ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Trainer", 0);
            saveProgress(player);
            return;
         
        }

        
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Tier",    nextTier);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Trainer", nextTrainer);
        saveProgress(player);
        
        
        String locStr = ConfigGetters.getRoomLocationFromTier(nextTier, nextTrainer);
        if (locStr == null || locStr.trim().isEmpty()) {

            player.sendMessage(new TextComponentString(TextFormatting.RED + "Sala ausente para Tier " + nextTier + " / Trainer " + nextTrainer));
            cancelChallenge(player);
            return;
        }

        String[] loc = locStr.split(",");
        int dimID = ConfigManager.getWorldID(loc[0]);
        BlockPos pos = new BlockPos(Integer.parseInt(loc[1]), Integer.parseInt(loc[2]), Integer.parseInt(loc[3]));

        TeleportHandler.teleportPlayer(player, dimID, loc[0], pos);
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Proximo treinador carregado! Tier " + nextTier + " – Treinador " + nextTrainer));

        org.spongepowered.api.Sponge.getScheduler().createTaskBuilder()
                .delayTicks(2)
                .execute(() -> placePlayerAtTrainer((EntityPlayerMP) player, nextTier, nextTrainer))
                .submit(pluginOwner());
    }

    /* ----------------------------------------------------------------- */
    /*  Pausa / cancela challenge                                         */
    /* ----------------------------------------------------------------- */
    public static void pauseChallenge(EntityPlayer player) throws ObjectMappingException {
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Is-Challenging", false);
        saveProgress(player);
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "Seu desafio foi pausado. Use /bt resume para continuar."));
    }

    public static void cancelChallenge(EntityPlayer player) throws ObjectMappingException {
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Is-Challenging", false);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Tier",    0);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Trainer", 0);
        saveProgress(player);
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "Desafio cancelado."));
    }
   

    /* ----------------------------------------------------------------- */
    /*  Helpers                                                           */
    /* ----------------------------------------------------------------- */
    public static void startBattle(EntityPlayer player, int tier, int trainerNum) {
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Tier",    tier);
        ConfigManager.setPlayerValue(player, "Current-Challenge-Info.Current-Trainer", trainerNum);
        saveProgress(player);
    }

    private static void saveProgress(EntityPlayer player) {
        try { ConfigManager.savePlayerConfig(player.getUniqueID()); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private static PluginContainer pluginOwner() {
        if (BattleTower.INSTANCE != null) {
            Optional<PluginContainer> opt = Sponge.getPluginManager().fromInstance(BattleTower.INSTANCE);
            if (opt.isPresent()) return opt.get();
        }
        return Sponge.getPluginManager().getPlugin("battletower")
                .orElseThrow(() -> new IllegalStateException("Plugin BattleTower nao carregado"));
    }

    private static void placePlayerAtTrainer(EntityPlayerMP player, int tier, int trainer) {
        String loc = ConfigGetters.getTrainerLocation(tier, trainer);
        if (loc == null || loc.trim().isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Trainer location invalida."));
            return;
        }

        String[] s = loc.split(",");
        String world = s[0].trim();
        BlockPos trainerPos = new BlockPos(
                Integer.parseInt(s[1].trim()),
                Integer.parseInt(s[2].trim()),
                Integer.parseInt(s[3].trim()));

        // só prossegue se já estiver no mesmo mundo
        if (!player.getEntityWorld().getWorldInfo().getWorldName().equals(world)) return;

        NPCTrainer trainerNPC = player.world.getEntitiesWithinAABB(
                NPCTrainer.class, new net.minecraft.util.math.AxisAlignedBB(trainerPos))
                .stream().filter(e -> e.getPosition().equals(trainerPos)).findFirst().orElse(null);

        if (trainerNPC == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "NPC invalido ou nao encontrado."));
            return;
        }

        // posiciona o player a 1 bloco de distância e "olhando" pro NPC (ajuda o engage)
        double px = trainerNPC.posX + 1.0;
        double py = trainerNPC.posY;
        double pz = trainerNPC.posZ;

        // calcula yaw pra encarar o NPC
        double dx = trainerNPC.posX - px;
        double dz = trainerNPC.posZ - pz;
        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        player.setPositionAndRotation(px, py, pz, yaw, player.rotationPitch);
        player.setPositionAndUpdate(px, py, pz);
    }
}
