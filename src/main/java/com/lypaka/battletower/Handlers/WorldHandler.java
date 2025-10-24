package com.lypaka.battletower.Handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

public class WorldHandler {

    public static String worldName;

    /**
     * Retorna o mundo atual do jogador no Sponge.
     */
    public static World getSpongeWorld(EntityPlayer player) {
        return ((Player) player).getWorld();
    }

    /**
     * Retorna o mundo atual do jogador no Forge padrão.
     */
    public static net.minecraft.world.World getForgeWorld(EntityPlayer player) {
        return player.getEntityWorld();
    }

        public static WorldServer getWorldByName(String worldName) {
        for (int id : DimensionManager.getStaticDimensionIDs()) {
            WorldServer ws = DimensionManager.getWorld(id);
            if (ws != null && ws.getWorldInfo().getWorldName().equalsIgnoreCase(worldName)) {
                return ws;
            }
        }
        return null;
    }
}
