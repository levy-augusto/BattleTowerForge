package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Handlers.PermissionHandler;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CommandListener {

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!(event.getSender() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSender();

        boolean allowCommands = ConfigManager.getConfigNode(0, new Object[]{"Misc", "Allow-Commands"}).getBoolean();
        boolean isChallenging = ConfigManager.getPlayerConfigNode(player.getUniqueID(), new Object[]{"Current-Challenge-Info", "Is-Challenging"}).getBoolean();
        boolean isAdmin = PermissionHandler.hasPermission(player, "battletower.command.admin");

        if (!allowCommands && isChallenging && !isAdmin) {
            event.setCanceled(true);
            player.sendMessage(FancyText.getFancyComponent("&eCommands while in the Battle Tower are not allowed!"));
        }
    }
}
