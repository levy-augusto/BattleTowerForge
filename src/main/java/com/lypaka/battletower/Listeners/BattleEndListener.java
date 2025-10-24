package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Guis.PromptGui;
import com.lypaka.battletower.Utils.BPHandler;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Config.TierHandler;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import info.pixelmon.repack.ninja.leaping.configurate.loader.ConfigurationLoader;
import info.pixelmon.repack.ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;

import com.pixelmonmod.pixelmon.storage.TrainerPartyStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

import java.io.IOException;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;


public class BattleEndListener {

    private static boolean trigger = false;

    @SubscribeEvent
    public void onBattleEnd(BattleEndEvent event) throws ObjectMappingException {
        BattleControllerBase bcb = event.bc;
        PlayerParticipant playerParticipant = null;
        TrainerParticipant trainerParticipant = null;

        if (bcb.participants.get(0) instanceof PlayerParticipant && bcb.participants.get(1) instanceof TrainerParticipant) {
            playerParticipant = (PlayerParticipant) bcb.participants.get(0);
            trainerParticipant = (TrainerParticipant) bcb.participants.get(1);
        } else if (bcb.participants.get(1) instanceof PlayerParticipant && bcb.participants.get(0) instanceof TrainerParticipant) {
            playerParticipant = (PlayerParticipant) bcb.participants.get(1);
            trainerParticipant = (TrainerParticipant) bcb.participants.get(0);
        }

        if (playerParticipant == null || trainerParticipant == null) return;

        EntityPlayerMP player = playerParticipant.player;
        String worldName;

        if (BattleTower.isSpongeLoaded) {
            Player spongePlayer = (Player) player;
            World spongeWorld = spongePlayer.getWorld();
            worldName = spongeWorld.getName();
        } else {
            worldName = player.world.getWorldInfo().getWorldName();
        }

        BlockPos pos = trainerParticipant.trainer.getPosition();
        String location = worldName + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        if (trainerParticipant.isDefeated) {
            if (ConfigGetters.isBattleTowerNPC(location)) {
                trigger = true;
                BPHandler.rewardBP(player, trainerParticipant.trainer.getName());
                ConfigManager.getPlayerConfigNode(player.getUniqueID(), new Object[]{
                        "NPC-Battle-Map", "Tier-" + ConfigGetters.getCurrentTier(player),
                        "Trainer-" + ConfigGetters.getCurrentTrainer(player)
                }).setValue(true);

                
                if (ConfigGetters.useExternalNPCConfigs()) {
                    int tier    = ConfigGetters.getCurrentTier(player);
                    int trainer = ConfigGetters.getCurrentTrainer(player);

                  TrainerPartyStorage storage = trainerParticipant.trainer.getPokemonStorage();

                  List<Pokemon> team = java.util.Arrays.stream(storage.getAll())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

             
                    List<String> specStrings = team.stream()
                            .filter(Objects::nonNull)
                            .map(p -> p.getSpecies().name() + ",lvl:" + p.getLevel())  
                            .collect(Collectors.toList());

                    Path file = Paths.get(BattleTower.MOD_CONFIG_DIR.toString(),
                                        "npcs", "Tier-" + tier,
                                        "trainer-" + trainer + ".conf");

                    try {
                        Files.createDirectories(file.getParent());
                        ConfigurationLoader<CommentedConfigurationNode> loader =
                                HoconConfigurationLoader.builder().setPath(file).build();
                        CommentedConfigurationNode root = loader.load();
                        root.getNode("Team").setValue(specStrings);
                        loader.save(root);
                        BattleTower.logger.info("[BT] Trainer config salvo: " + file);
                    } catch (IOException e) {
                        BattleTower.logger.error("Não foi possível salvar trainer config", e);
                    }
                }




            } else {
                String headBossLocation = ConfigManager.getConfigNode(2, new Object[]{"NPC-Settings", "Head-Boss-Location"}).getString();
                if (headBossLocation.equalsIgnoreCase(location)) {
                    int reward = ConfigManager.getConfigNode(0, new Object[]{"Battle-Points", "Tower-Head-Reward"}).getInt();
                    BPHandler.rewardBP(player, reward);
                    BattleTower.server.getPlayerList().sendMessage(FancyText.getFancyComponent("&e" + player.getName() + " has conquered the Battle Tower!"));
                    TierHandler.cancelChallenge(player);
                }
            }
        } else if (ConfigGetters.isBattleTowerNPC(location)) {
            TierHandler.cancelChallenge(player);
        }

            try {
                ConfigManager.savePlayerConfig(player.getUniqueID());
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        if (!event.getContainer().toString().contains("com.pixelmonmod.pixelmon.gui") || !ConfigGetters.isInTower(player) || !trigger)
            return;

        int tier = ConfigGetters.getCurrentTier(player);
        int trainer = ConfigGetters.getCurrentTrainer(player);
        boolean pause = false;

        if (trainer == ConfigGetters.getTrainerPerRoundAmount()) {
            if (tier == ConfigGetters.getTierAmount()) {
                player.sendMessage(FancyText.getFancyComponent("&bThe Tower Head has challenged you to a battle!"));
            } else {
                ConfigManager.getPlayerConfigNode(player.getUniqueID(), new Object[]{"Current-Challenge-Info", "Current-Tier"}).setValue(tier + 1);
                ConfigManager.getPlayerConfigNode(player.getUniqueID(), new Object[]{"Current-Challenge-Info", "Current-Trainer"}).setValue(1);
                ConfigManager.savePlayer(player.getUniqueID());
                pause = ConfigManager.getConfigNode(0, new Object[]{"Misc", "Allow-Pausing"}).getBoolean();
            }
        } else {
            ConfigManager.getPlayerConfigNode(player.getUniqueID(), new Object[]{"Current-Challenge-Info", "Current-Trainer"}).setValue(trainer + 1);
            ConfigManager.savePlayer(player.getUniqueID());
        }

        PromptGui.showPromptMenu(player, pause);
        trigger = false;
    }
}
