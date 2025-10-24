package com.lypaka.battletower.Utils;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import java.io.IOException;



public class RewardHandler {

public static final java.util.Map<java.util.UUID, NPCTrainer> rewardMap = new java.util.HashMap<>();

    public static void rewardBP(EntityPlayer player, int amount) {

        int current = ConfigGetters.getCurrentBP(player);
        ConfigGetters.setBP(player, current + amount);

        try {                                           
            ConfigManager.savePlayerConfig(player.getUniqueID());
        } catch (IOException e) {
            e.printStackTrace();                       
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Você recebeu " + amount + " BP por vencer!"));
    }

    public static void giveBonusReward(EntityPlayer player,
                                       String rewardType, String value) {

        switch (rewardType.toLowerCase()) {
            case "item":
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "Você recebeu um item especial: " + value));
                
                break;

            case "pokemon":
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "Você ganhou um Pokémon raro: " + value));
                
                break;

            default:
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "Recompensa desconhecida: " + rewardType));
        }
    }
}
