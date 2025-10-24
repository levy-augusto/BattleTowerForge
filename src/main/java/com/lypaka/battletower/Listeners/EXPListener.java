package com.lypaka.battletower.Listeners;

import com.lypaka.battletower.Config.ConfigGetters;
import com.pixelmonmod.pixelmon.api.events.ExperienceGainEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EXPListener {

    @SubscribeEvent
    public void onEXPGain(ExperienceGainEvent event) {
        
        EntityPlayerMP owner = event.pokemon.getPlayerOwner(); 
        if (owner != null && ConfigGetters.isInTower(owner)) {
            event.setExperience(0);
        }
    }

    @SubscribeEvent
    public void onPlayerEXPGain(BattleEndEvent event) {
        
            for (EntityPlayerMP player : event.getPlayers()) {
            if (ConfigGetters.isInTower(player)) {
                
            }
    }
        
    }
}
