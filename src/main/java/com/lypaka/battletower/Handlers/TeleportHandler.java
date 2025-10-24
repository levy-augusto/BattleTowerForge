// src/main/java/com/lypaka/battletower/Handlers/TeleportHandler.java
package com.lypaka.battletower.Handlers;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.Teleporter;
import com.lypaka.battletower.random.RandomTeamApplier;

import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.List;

public class TeleportHandler {
    public static Logger logger = LogManager.getLogger("Battle Tower");
    /**
     * Tenta aplicar time aleatório do random.conf ao trainer mais próximo do destino (antes do TP).
     * Só roda se o RandomTeamApplier estiver inicializado (e ele próprio checa se está enabled).
     */
    private static void maybeApplyRandomTeamBeforeTeleport(EntityPlayer player, String worldName, BlockPos dest) {
        // Se o serviço não existe, ignora
        RandomTeamApplier applier = BattleTower.getTeamApplier();
        if (applier == null) return;

        // Tier atual do jogador (definido no fluxo da challenge)
        int tier = ConfigGetters.getCurrentTier(player);
        if (tier <= 0) return;

        // Pega o mundo Forge (mesmo com Sponge presente) para varrer entidades
        int dimID = ConfigManager.getWorldID(worldName);
        net.minecraft.world.World mcWorld = DimensionManager.getWorld(dimID);
        if (mcWorld == null) return;

        // Garante chunk carregado o suficiente para encontrar entidades
        try {
        if (mcWorld instanceof WorldServer) {
            WorldServer ws = (WorldServer) mcWorld;
            int cx = dest.getX() >> 4;
            int cz = dest.getZ() >> 4;
            // força carregar/criar o chunk do destino
            ws.getChunkProvider().provideChunk(cx, cz);
        }
    } catch (Throwable ignored) {}

        // Procura um NPCTrainer perto do destino (raio 16 blocos é suficiente na maioria das salas)
        double radius = 16.0D;
        AxisAlignedBB box = new AxisAlignedBB(dest).grow(radius);
        List<NPCTrainer> trainers = mcWorld.getEntitiesWithinAABB(NPCTrainer.class, box);
        if (trainers == null || trainers.isEmpty()) return;

        // Pega o primeiro (ou, se preferir, o mais próximo)
        NPCTrainer trainer = trainers.get(0);
        logger.info("[BT-Random] Teleport detectado para tier=" + tier + " world=" + worldName + " pos=" + dest);
        logger.info("[BT-Random] NPCs encontrados: " + trainers.size());

        // Aplica time aleatório (trainerKey = null -> usa defaultPool do tier; teamSize default = 3)
        try {
                logger.info("[BT-Random] applyIfAvailable chamado para NPC=" + trainer.getName());


            applier.applyIfAvailable(trainer, tier, null, 3);
        } catch (Throwable t) {
                logger.error("[BT-Random] erro ao aplicar time aleatorio", t);

            // silencioso para não travar TP se algo der errado
        }
    }

    public static void teleportPlayer(EntityPlayer player, int dimID, String worldName, BlockPos pos) {

        // >>> Antes de teleportar, tenta preparar o NPC da sala com time randômico (se habilitado)
        try {
            maybeApplyRandomTeamBeforeTeleport(player, worldName, pos);
        } catch (Throwable ignored) {}

        // Caso seja Sponge
        if (BattleTower.isSpongeLoaded) {
            Sponge.getServer().getWorld(worldName).ifPresent(world -> {
                Location<World> tpLoc = new Location<>(world, pos.getX(), pos.getY(), pos.getZ());
                ((Player) player).setLocation(tpLoc);
            });
            return;
        }

        // Caso seja Forge padrão (sem Sponge)
        Teleporter tp = new Teleporter();
        net.minecraft.world.World world;

        if (BattleTower.isForgeEssentialsLoaded) {
            // Se você usa um WorldHandler próprio, pode trocar essa linha:
            // world = WorldHandler.getWorldByName(worldName);
            // Fallback para o dim se o acima não estiver disponível:
            world = DimensionManager.getWorld(dimID);
        } else {
            world = DimensionManager.getWorld(dimID);
        }

        if (world != null) {
            tp.placeEntity(world, player, player.rotationYaw);
            player.setPosition(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    // Teleporta para a entrada (sem alterar NPCs; o helper não encontra nenhum e sai)
    public static void teleportToEntrance(EntityPlayer player) {
        String loc = ConfigGetters.getEntranceLocation();
        if (loc == null || loc.trim().isEmpty()) {
            player.sendMessage(new TextComponentString("§cNenhuma entrada definida! Use /bt setentrance"));
            return;
        }

        String[] s = loc.split(",");
        String world = s[0].trim();
        BlockPos pos = new BlockPos(
                Integer.parseInt(s[1].trim()),
                Integer.parseInt(s[2].trim()),
                Integer.parseInt(s[3].trim()));
        int dimID = ConfigManager.getWorldID(world);

        teleportPlayer(player, dimID, world, pos);
    }
}
