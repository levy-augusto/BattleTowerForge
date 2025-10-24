package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.TierHandler;
import com.lypaka.battletower.Utils.RewardHandler;
import com.pixelmonmod.pixelmon.api.events.BattleStartedEvent;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class BattleStartListener {

    @SubscribeEvent
    public void onBattleStart(BattleStartedEvent evt) {


        BattleControllerBase controller = evt.bc;  
        if (controller.battleEnded) return; 

            // procura por PlayerParticipant + TrainerParticipant
            PlayerParticipant  playerPart  = null;
            TrainerParticipant trainerPart = null;

            for (BattleParticipant bp : controller.participants) {
                if (bp instanceof PlayerParticipant)   playerPart  = (PlayerParticipant)  bp;
                if (bp instanceof TrainerParticipant) trainerPart = (TrainerParticipant) bp;
            }
            if (playerPart == null || trainerPart == null) return;

            /* ------------------------------------------ */
            if (!(playerPart.getEntity() instanceof EntityPlayer)) return;
            EntityPlayer player   = (EntityPlayer) playerPart.getEntity();
            NPCTrainer   trainer  = (NPCTrainer) trainerPart.getEntity();

            // int tier       = TierHandler.getTierFromLocation(trainer);
            int tier = ConfigGetters.getCurrentTier(player);
            // int trainerNum = TierHandler.getTrainerNumberFromLocation(trainer);
            int trainerNum = ConfigGetters.getCurrentTrainer(player);

            TierHandler.startBattle(player, tier, trainerNum);

            /* ---------- Open-Level ----------- */
            if ("Open Level".equalsIgnoreCase(ConfigGetters.getCurrentMode(player))) {

                
                PlayerPartyStorage storage = Pixelmon.storageManager.getParty(player.getUniqueID());
                if (storage != null) {
                    // percorre os Pokémon que já estão na batalha
                    playerPart.controlledPokemon.forEach(wrapper -> {
                        EntityPixelmon poke = wrapper.entity;      // entidade viva em batalha
                        if (poke != null) {
                            poke.getLvl().setLevel(ConfigGetters.getMinOpenLevel());
                            poke.updateStats();
                        }
                    });
                    // também garante nível mínimo na Box do jogador
                    storage.getTeam().forEach(pkmn -> {
                        if (pkmn != null && pkmn.getLevel() < ConfigGetters.getMinOpenLevel()) {
                            pkmn.setLevel(ConfigGetters.getMinOpenLevel());
                        }
                    });
                    // storage.update();
                }
            }

            // registra para recompensa após a vitória
            RewardHandler.rewardMap.put(player.getUniqueID(), trainer);

        }
    }