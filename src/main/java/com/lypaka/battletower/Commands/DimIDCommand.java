package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Handlers.PermissionHandler;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class DimIDCommand extends CommandBase {

    @Override
    public String getName() {
        return "dim";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt dim";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;

            if (!PermissionHandler.hasPermission(player, "battletower.command.admin")) {
                player.sendMessage((FancyText.getFancyComponent("&cYou do not have permission to use this command!")));
                return;
            }

            int dimensionID = player.dimension;
            player.sendMessage(FancyText.getFancyComponent("&eYour current dimension ID is: " + dimensionID));
        } else {
            sender.sendMessage(new TextComponentString("Only players can use this command."));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // permissionado via LuckPerms (Sponge), não via OP level
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
                "battletower.admin.dim",
                "battletower.admin.*"
        );
    }
}
