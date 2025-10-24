package com.lypaka.battletower.Guis;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

public class BorderPage {
   public static ItemStack empty() {
      ItemStack empty = ItemStack.builder().itemType(ItemTypes.STAINED_GLASS_PANE).build();
      empty.offer(Keys.DISPLAY_NAME, Text.of());
      empty.offer(Keys.DYE_COLOR, DyeColors.RED);
      return empty;
   }
}