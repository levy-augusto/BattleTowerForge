// SetTrainerCommand.java
package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigGetters;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class SetTrainerCommand extends CommandBase {

    @Override
    public String getName() {
        return "settrainer";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bt settrainer <tier> <trainer>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) return;
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("§cUso: /bt settrainer <tier> <trainer>"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        String world = player.getEntityWorld().getWorldInfo().getWorldName();
        BlockPos pos = player.getPosition();

        String coord = world + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        int tier = Integer.parseInt(args[0]);
        int trainer = Integer.parseInt(args[1]);

        try {
            ConfigGetters.setTrainerLocation(tier, trainer, coord);
            sender.sendMessage(new TextComponentString("§aTrainer " + trainer + " do Tier " + tier + " setado com sucesso!"));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§cErro ao salvar no arquivo!"));
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
            "battletower.admin.settrainer",
            "battletower.admin.*"
    );
}
}
