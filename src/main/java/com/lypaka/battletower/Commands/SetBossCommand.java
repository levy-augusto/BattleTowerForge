package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigManager;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class SetBossCommand extends CommandBase {

    @Override
    public String getName() { return "setboss"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/battletower setboss"; }


    @Override
    public void execute(MinecraftServer server,
                        ICommandSender sender,
                        String[] args) throws CommandException {

        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§cApenas jogadores podem usar este comando."));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        BlockPos pos       = player.getPosition();

        String locString = player.world.getWorldInfo().getWorldName()
                + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        // grava no mesmo caminho e formato que o listener usa
        ConfigurationNode node = ConfigManager.getConfigNode(
                2, "NPC-Settings", "Head-Boss-Location");
        node.setValue(locString);

        try {
            ConfigManager.saveAll();
            player.sendMessage(new TextComponentString("§aLocalização do Head Boss registrada!"));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(new TextComponentString("§cErro ao salvar localização do Head Boss."));
        }
    }

    @Override
public int getRequiredPermissionLevel() {
    return 0; // permissionado via LuckPerms (Sponge), não via OP level
}

@Override
public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    return com.lypaka.battletower.Utils.Perms.has(sender,
            "battletower.admin.setboss",
            "battletower.admin.*"
    );
}
}
