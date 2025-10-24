package com.lypaka.battletower.Utils;


import com.lypaka.battletower.Config.ConfigManager;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Locale;

public class ItemCreator {

    public static void giveTM(Player player, String moveName, int amount) {
        giveDisc(player, moveName, amount, "TM", 0);
    }

    public static void giveTR(Player player, String moveName, int amount) {
        giveDisc(player, moveName, amount, "TR", 1);
    }

    public static void giveHM(Player player, String moveName, int amount) {
        giveDisc(player, moveName, amount, "HM", 2);
    }

    private static void giveDisc(Player player, String moveName, int amount,
                                 String prefix, int configIndex) {

        String cleanedMove = moveName.replace("[", "")
                                     .replace("]", "")
                                     .trim();

        // valida se o golpe está listado na config
        if (!ConfigManager.getConfigNode(configIndex, prefix + "s")
                          .getValue()
                          .toString()
                          .toLowerCase(Locale.ROOT)
                          .contains(cleanedMove.toLowerCase(Locale.ROOT))) {
            return;
        }

        int number = ConfigManager.getConfigNode(configIndex,
                                                 prefix + "s",
                                                 cleanedMove,
                                                 "Number").getInt();

        String displayName = String.format("%s%02d: %s",
                                           prefix,
                                           number,
                                           cleanedMove);

        String itemID = "pixelmon:" + prefix.toLowerCase() + String.format("%02d", number);

        // cria o ItemStack NMS e aplica NBT
        net.minecraft.item.ItemStack mcItem =
                new net.minecraft.item.ItemStack(net.minecraft.item.Item.getByNameOrId(itemID));

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("HideTooltip", true);          // oculta tooltip vanilla
        mcItem.setTagCompound(tag);

        // converte para ItemStack da SpongeAPI
        ItemStack spongeItem = (ItemStack) (Object) mcItem;
        spongeItem = spongeItem.copy();               // segurança
        spongeItem.offer(Keys.DISPLAY_NAME,
                         Text.of(TextColors.WHITE, displayName));
        spongeItem.setQuantity(amount);

        player.getInventory().offer(spongeItem);
    }
}
