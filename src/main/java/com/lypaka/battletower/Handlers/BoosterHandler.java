package com.lypaka.battletower.Handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;

import java.util.HashSet;
import java.util.Set;

public class BoosterHandler {

    // Lista de jogadores com booster ativo
    private static final Set<String> boostedPlayers = new HashSet<>();

    // Método para ativar booster em um jogador
    public static void activateBooster(EntityPlayer player) {
        boostedPlayers.add(player.getUniqueID().toString());
    }

    // Método para desativar booster
    public static void deactivateBooster(EntityPlayer player) {
        boostedPlayers.remove(player.getUniqueID().toString());
    }

    // Verifica se o jogador tem booster ativo
    public static boolean hasBooster(EntityPlayer player) {
        return boostedPlayers.contains(player.getUniqueID().toString());
    }

    // Aplica o booster ao valor base se o jogador tiver booster ativo
    public static int applyBooster(EntityPlayer player, int baseValue, ConfigurationNode boosterConfig) {
        if (!hasBooster(player)) return baseValue;

        String[] parts = boosterConfig.getString("add 0").split(" ");
        String operation = parts[0];
        int amount = Integer.parseInt(parts[1]);

        if (operation.equalsIgnoreCase("multiply")) {
            return baseValue * amount;
        } else {
            return baseValue + amount;
        }
    }
    
    public static ItemStack getBoosterItem(String id) {
        ItemStack stack = new ItemStack(Item.getByNameOrId("minecraft:nether_star")); // Item visual do booster
        stack.setStackDisplayName("§6Booster: " + id); // Nome colorido

        // Adiciona lore
        NBTTagCompound display = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("§7Clique para ativar este booster."));
        lore.appendTag(new NBTTagString("§eID: " + id));
        display.setTag("Lore", lore);
        stack.setTagInfo("display", display);

        return stack;
    }

}
