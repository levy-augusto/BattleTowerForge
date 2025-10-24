package com.lypaka.battletower.Utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Optional;

public class Perms {

    /** true = console/commandblock tem permissão sempre */
    public static boolean has(ICommandSender sender, String... nodes) {
        if (!(sender instanceof EntityPlayerMP)) return true; // console/rcon etc

        EntityPlayerMP nms = (EntityPlayerMP) sender;
        Optional<Player> sp = Sponge.getServer().getPlayer(nms.getUniqueID());
        if (!sp.isPresent()) return false;

        Player p = sp.get();
        for (String node : nodes) {
            if (p.hasPermission(node)) return true;
            // suporte a wildcard simples: battletower.admin.*
            if (node.endsWith(".*")) {
                // nada extra — LuckPerms resolve o wildcard
                if (p.hasPermission(node)) return true;
            }
        }
        return false;
    }
}
