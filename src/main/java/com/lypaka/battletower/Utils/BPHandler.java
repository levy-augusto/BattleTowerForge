package com.lypaka.battletower.Utils;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Handlers.BoosterHandler;
import net.minecraft.entity.player.EntityPlayer;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class BPHandler {

    public static void rewardBP(EntityPlayer player, String trainerID) throws ObjectMappingException {
        int bp = getBaseBP(trainerID);
        int currentBP = ConfigGetters.getCurrentBP(player);

        ConfigurationNode boosterConfig = ConfigGetters.getBoosterNode();
        int newBP = BoosterHandler.applyBooster(player, currentBP + bp, boosterConfig);

        ConfigGetters.setBP(player, newBP);

        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ((net.minecraft.entity.player.EntityPlayerMP) player)
                    .sendMessage(FancyText.getFancyComponent("&bYou received " + bp + " BP!"));
        }
    }

    public static void rewardBP(EntityPlayer player, int amount) throws ObjectMappingException {
        int currentBP = ConfigGetters.getCurrentBP(player);
        ConfigurationNode boosterConfig = ConfigGetters.getBoosterNode();
        int newBP = BoosterHandler.applyBooster(player, currentBP + amount, boosterConfig);

        ConfigGetters.setBP(player, newBP);

        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ((net.minecraft.entity.player.EntityPlayerMP) player)
                    .sendMessage(FancyText.getFancyComponent("&bYou received " + amount + " BP!"));
        }
    }

    // Método auxiliar opcional se for usar ID de trainer
    private static int getBaseBP(String trainerID) {
        // Valor fixo de exemplo — você pode alterar isso para ler de um config específico
        return 5;
    }
}
