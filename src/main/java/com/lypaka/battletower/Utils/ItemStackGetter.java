package com.lypaka.battletower.Utils;

import com.lypaka.battletower.Config.ConfigManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ItemStackGetter {

    public static ItemStack getDisc(String disc, String move) {
        String cleanedMove = move.replace("[", "").replace("]", "");

        int configIndex;
        switch (disc.toUpperCase()) {
            case "TM":
                configIndex = 0;
                break;
            case "TR":
                configIndex = 1;
                break;
            case "HM":
                configIndex = 2;
                break;
            default:
                return ItemStack.builder().build();
        }

        int number = ConfigManager.getConfigNode(configIndex, disc + "s", cleanedMove, "Number").getInt();
        String formattedNumber = String.format("%02d", number);
        String itemID = "pixelmon:" + disc.toLowerCase() + formattedNumber;
        String displayName = disc.toUpperCase() + formattedNumber + ": " + cleanedMove;

        // Criar o item do Minecraft
        net.minecraft.item.ItemStack mcItem = new net.minecraft.item.ItemStack(Item.getByNameOrId(itemID));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("HideTooltip", true);
        mcItem.setTagCompound(tag);

        // Converter para SpongeAPI
        ItemStack spongeItem = (ItemStack) (Object) mcItem;
        spongeItem = spongeItem.copy();
        spongeItem.offer(Keys.DISPLAY_NAME, Text.of(TextColors.WHITE, displayName));

        return spongeItem;
    }
}
