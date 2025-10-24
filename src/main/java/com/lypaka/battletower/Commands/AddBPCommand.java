package com.lypaka.battletower.Commands;

import java.io.IOException;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Handlers.PermissionHandler;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class AddBPCommand extends CommandBase {

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt add <player> <amount>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length < 3) {
            sender.sendMessage(FancyText.getFancyComponent("&cUsage: /bt add <player> <amount>"));
            return;
        }

        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            if (!PermissionHandler.hasPermission(player, "battletower.command.admin")) {
                player.sendMessage(FancyText.getFancyComponent("&cYou do not have permission to use this command!"));
                return;
            }
        }

        String playerName = args[1];
        EntityPlayer target = server.getPlayerList().getPlayerByUsername(playerName);

        if (target == null) {
            sender.sendMessage(FancyText.getFancyComponent("&eInvalid target!"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(FancyText.getFancyComponent("&cAmount must be a number!"));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(FancyText.getFancyComponent("&eCannot add a non-positive number of BP to the target!"));
            return;
        }

        int currentBP = ConfigGetters.getCurrentBP(target);
        ConfigGetters.setBP(target, currentBP + amount);
        try {
            ConfigManager.savePlayerConfig(target.getUniqueID());
        } catch (IOException e) {
            e.printStackTrace();
        }

        target.sendMessage(FancyText.getFancyComponent("&eYou received " + amount + " BP!"));
        sender.sendMessage(FancyText.getFancyComponent("&aSuccessfully sent " + target.getName() + " " + amount + " BP!"));
    }


    @Override
    public int getRequiredPermissionLevel() {
        return 0; // permissionado via LuckPerms (Sponge), não via OP level
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
                "battletower.admin.addbp",
                "battletower.admin.*"
        );
    }

}
