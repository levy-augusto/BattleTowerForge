package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CheckCommand extends CommandBase {

    @Override
    public String getName() {
        return "check";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt check";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            int amount = ConfigGetters.getCurrentBP(player);
            player.sendMessage(FancyText.getFancyComponent("&eYou currently have " + amount + " BP."));
        } else {
            sender.sendMessage(new TextComponentString("Only players can run this command."));
        }
    }

        @Override
    public int getRequiredPermissionLevel() {
        return 0; // permissionado via LuckPerms (Sponge), não via OP level
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
                "battletower.command.check",
                "battletower.admin.*"
        );
    }
}
