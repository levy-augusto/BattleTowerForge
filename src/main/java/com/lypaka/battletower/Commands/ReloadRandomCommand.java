package com.lypaka.battletower.Commands;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class ReloadRandomCommand extends CommandBase {

    @Override
    public String getName() {
        return "reloadrandom";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/battletower reloadrandom";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        try {
            ConfigManager.reloadRandom();
            BattleTower.initRandomTeamServices();
            sender.sendMessage(new TextComponentString("§a[BT] random-pokemon.conf recarregado!"));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§c[BT] Falha ao recarregar random-pokemon.conf: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // permissionado via LuckPerms (Sponge), não via OP level
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
                "battletower.admin.reloadrandom",
                "battletower.admin.*"
        );
    }
}
