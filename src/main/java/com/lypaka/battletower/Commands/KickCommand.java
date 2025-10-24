package com.lypaka.battletower.Commands;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Handlers.PermissionHandler;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Config.TierHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class KickCommand extends CommandBase {

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt kick <player>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // Verifica se o comando está sendo executado por um jogador e se ele tem permissão
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            if (!PermissionHandler.hasPermission(player, "battletower.command.admin")) {
                player.sendMessage((FancyText.getFancyComponent("&cYou do not have permission to use this command!")));
                return;
            }
        }

        if (args.length < 1) {
            sender.sendMessage((FancyText.getFancyComponent("&cUsage: /bt kick <player>")));
            return;
        }

        String targetName = args[0];
        EntityPlayer target = BattleTower.server.getPlayerList().getPlayerByUsername(targetName);

        if (target == null) {
            sender.sendMessage((FancyText.getFancyComponent("&eInvalid target!")));
            return;
        }

        try {
            TierHandler.cancelChallenge(target);
            sender.sendMessage((FancyText.getFancyComponent("&ePlayer " + target.getName() + " has been kicked from the challenge.")));
        } catch (ObjectMappingException e) {
            e.printStackTrace();
            sender.sendMessage((FancyText.getFancyComponent("&cAn error occurred while kicking the player.")));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // permissionado via LuckPerms (Sponge), não via OP level
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
                "battletower.admin.kick",
                "battletower.admin.*"
        );
    }
}
