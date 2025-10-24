// com.lypaka.battletower.Listeners.CommandInterceptor.java
package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.State.SurrenderManager;
import com.lypaka.battletower.Config.ConfigGetters; 
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CommandInterceptor {

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!(event.getSender() instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSender();
        String cmdName = event.getCommand() != null ? event.getCommand().getName() : "";
        if (!"endbattle".equalsIgnoreCase(cmdName)) return;

        // So aplica dentro da Battle Tower: jogador precisa estar em challenge ativo
        if (!ConfigGetters.isChallenging(player)) return;

        // Marca desistência e deixa o Pixelmon encerrar a batalha normalmente
        SurrenderManager.mark(player.getUniqueID());

        // Mensagem sem acentos (preferencia in-game do seu server)
        player.sendMessage(new TextComponentString(
            TextFormatting.RED + "Voce desistiu do desafio da Battle Tower. A batalha sera encerrada e voce sera enviado a recepcao."
        ));
        // Nao cancele: event.setCanceled(true) // -> NAO usar, deixe o Pixelmon terminar a batalha
    }
}
