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

import java.util.*;

public class SetReceptionCommand extends CommandBase {

    @Override
    public String getName() {
        return "setreception";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/battletower setreception";
    }


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

       ConfigurationNode listNode = ConfigManager.getConfigNode(
        2, "NPC-Settings", "Receptionist-Locations");   // caminho certo

        List<String> list;
        try {

            List<String> existing = listNode.getList(          // carrega lista existente
            node -> ((ConfigurationNode) node).getString(),    // converte cada filho para String
            new ArrayList<String>());                          // valor padrão (mutável)

            list = new ArrayList<String>(existing);            // copia para lista mutável

        } catch (Exception ex) {
            ex.printStackTrace();
            list = new ArrayList<String>();
        }

        if (!list.contains(locString)) {
            list.add(locString);
        }

        listNode.setValue(list); 

        ConfigManager.saveAll();
        player.sendMessage(new TextComponentString("§aRecepcionista registrada!"));
    }

    @Override
public int getRequiredPermissionLevel() {
    return 0; // permissionado via LuckPerms (Sponge), não via OP level
}

@Override
public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    return com.lypaka.battletower.Utils.Perms.has(sender,
            "battletower.admin.setreception",
            "battletower.admin.*"
    );
}
}
