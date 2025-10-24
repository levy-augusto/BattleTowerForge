package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Config.TierHandler;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import com.lypaka.battletower.Utils.FancyText;

public class HeadBossClickListener {

    @Listener
    public void onInteract(InteractEntityEvent.Secondary.MainHand event, @Root Player spongePlayer) {
        Entity entity = (Entity) event.getTargetEntity();
        if (!(entity instanceof NPCTrainer)) return;

        World world = entity.getEntityWorld();
        BlockPos pos = entity.getPosition();
        String location = world.getWorldInfo().getWorldName() + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        try {
            String configLocation = ConfigManager.getConfigNode(2, "NPC-Settings", "Head-Boss-Location").getString();
            if (!location.equals(configLocation)) return;

            event.setCancelled(true);
            int finalTier = ConfigGetters.getTierAmount();
            int finalTrainer = ConfigGetters.getTrainerPerRoundAmount();

            TierHandler.startBattle((net.minecraft.entity.player.EntityPlayer) spongePlayer, finalTier, finalTrainer);
            spongePlayer.sendMessage(FancyText.getFancyText("&6Prepare-se para o desafio final contra o Chefe da Torre!"));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
