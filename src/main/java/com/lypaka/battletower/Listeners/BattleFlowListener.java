// src/main/java/com/lypaka/battletower/Listeners/BattleFlowListener.java
package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager; // <--- importa para salvar BP
import com.lypaka.battletower.Config.TierHandler;
import com.lypaka.battletower.Handlers.TeleportHandler;
import com.lypaka.battletower.State.SurrenderManager;
import com.lypaka.battletower.BattleTower;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.enums.battle.BattleResults;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;   // <--- mensagem pro jogador
import net.minecraft.util.text.TextFormatting;        // <--- cor da mensagem
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

public class BattleFlowListener {

    private void runNextTick(Runnable r) {
        try {
            if (BattleTower.isSpongeLoaded) {
                PluginContainer owner = Sponge.getPluginManager().getPlugin("battletower").orElse(null);
                if (owner != null) {
                    Sponge.getScheduler().createTaskBuilder().delayTicks(2).execute(r).submit(owner);
                    return;
                }
            }
        } catch (Throwable ignore) {}
        // fallback forge
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(r);
    }

    @SubscribeEvent
    public void onBattleEnd(BattleEndEvent event) {      

        for (EntityPlayerMP p : event.getPlayers()) {
            if (!ConfigGetters.isChallenging(p)) {                
                continue;
            }

            if (SurrenderManager.consume(p.getUniqueID())) {
                runNextTick(() -> {
                    try {
                        TierHandler.cancelChallenge(p); // encerra desafio sem subir tier/BP
                    } catch (Exception e) { e.printStackTrace(); }
                    TeleportHandler.teleportToEntrance(p); // tua “recepção”                
                });
                continue; // NADA de logica de vitoria/derrota abaixo
            }

            BattleResults result = event.results.getOrDefault(p.getUniqueID(), BattleResults.DRAW);        
            boolean won;
            if (result == BattleResults.VICTORY) {
                won = true;
            } else if (result == BattleResults.DRAW) {
                int able = Pixelmon.storageManager.getParty(p).countAblePokemon();
                won = (able > 0);               
            } else {
                won = false;
            }

            if (won) {
                runNextTick(() -> {
                    try {
                        // +1 BP pela vitória
                        int curr = ConfigGetters.getCurrentBP(p);
                        ConfigGetters.setBP(p, curr + 1);
                        ConfigManager.savePlayerConfig(p.getUniqueID());
                        p.sendMessage(new TextComponentString(TextFormatting.AQUA + "+1 BP pela vitoria na Battle Tower!"));                       
                        TierHandler.nextChallenge(p);
                    } catch (Exception e) { 
                        e.printStackTrace();
                    }
                });
            } else {
                runNextTick(() -> {
                    TeleportHandler.teleportToEntrance(p);
                    try { TierHandler.cancelChallenge(p); } catch (Exception e) { e.printStackTrace(); }
                });
            }
        }
    }
}

        