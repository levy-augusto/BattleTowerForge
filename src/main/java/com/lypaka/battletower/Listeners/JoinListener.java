package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Guis.PromptGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

import java.io.IOException;

public class JoinListener {

    @SubscribeEvent
    public void onJoin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        /* Carrega (ou cria) o arquivo de configuração do jogador */
        try {
            ConfigManager.createPlayerConfig(player.getUniqueID());
        } catch (IOException e) {
            e.printStackTrace();
            return;               // aborta para evitar NullPointer se não conseguiu criar
        }

        /* Inicializa o campo Next-Challenge-Time, se não existir */
        if (ConfigManager.getPlayerConfigNode(player.getUniqueID(), "Next-Challenge-Time").isVirtual()) {
            ConfigManager.getPlayerConfigNode(player.getUniqueID(), "Next-Challenge-Time")
                         .setValue("time");
            ConfigManager.savePlayer(player.getUniqueID());
        }

        /* Se havia desafio em pausa, mostra o menu de retomada */
        boolean isChallenging = ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                                "Current-Challenge-Info", "Is-Challenging").getBoolean(false);
        boolean isPaused      = ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                                "Current-Challenge-Info", "Is-Paused").getBoolean(false);

        if (isChallenging && isPaused) {
            PromptGui.showPromptMenu(player, true);
        }
    }
}
