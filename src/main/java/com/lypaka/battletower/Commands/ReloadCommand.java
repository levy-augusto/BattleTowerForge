package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Handlers.PermissionHandler;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReloadCommand extends CommandBase {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt reload";
    }

   @Override
public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

    // permissão
    if (sender instanceof EntityPlayer) {
        EntityPlayer p = (EntityPlayer) sender;
        if (!PermissionHandler.hasPermission(p, "battletower.command.admin")) {
            p.sendMessage(FancyText.getFancyComponent("&cYou do not have permission to use this command!"));
            return;
        }
    }

    try {
        /* --- re-carrega todas as configs --- */
        Path tmPath = Paths.get("config", "tmfactory");          // mesmo diretório que usou no bootstrap
        ConfigManager.setupAll(tmPath, "default");               // passe as pastas de NPCs que usa
        sender.sendMessage(FancyText.getFancyComponent("&aBattle Tower configuration reloaded with success!"));
    } catch (IOException e) {
        sender.sendMessage(FancyText.getFancyComponent("&cFailed to reload configs; see console."));
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
            "battletower.admin.reload",
            "battletower.admin.*"
    );
}

}
