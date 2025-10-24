package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Config.TierHandler;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;

public class TrainerClickListener {

    @Listener
    public void onTrainerClick(InteractEntityEvent.Secondary.MainHand event, @Root Player spongePlayer) throws ObjectMappingException {
        Entity entity = (Entity) event.getTargetEntity();
        if (!(entity instanceof NPCTrainer)) return;

        World world = entity.getEntityWorld();
        BlockPos pos = entity.getPosition();
        String location = world.getWorldInfo().getWorldName() + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        for (int tier = 1; tier <= 3; tier++) {
            for (int trainer = 1; trainer <= 7; trainer++) {
                String expectedLoc = ConfigManager
                .getConfigNode(2, "NPC-Settings", "Trainer-Locations", "Tier-" + tier, "Trainer-" + trainer)
                .getString();

                if (location.equalsIgnoreCase(expectedLoc)) {
                    event.setCancelled(true);
                    EntityPlayer player = (EntityPlayer) spongePlayer;

                    TierHandler.startBattle(player, tier, trainer);
                    player.sendMessage(new TextComponentString("§aDesafio contra o Treinador " + trainer + " do Tier " + tier + " iniciado!"));
                    return;
                }
            }
        }
    }
}
