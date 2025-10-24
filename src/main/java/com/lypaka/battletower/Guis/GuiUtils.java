package com.lypaka.battletower.Guis;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public final class GuiUtils {

    private GuiUtils() {}

    /** Cria um vidro tingido com nome em branco e HideFlags. */
    public static ItemStack pane(int meta) {
        ItemStack stack = new ItemStack(Blocks.STAINED_GLASS_PANE, 1, meta);
        stack.setStackDisplayName(" ");                         // tooltip vazio
        stack.setTagInfo("HideFlags", new net.minecraft.nbt.NBTTagInt(63)); // oculta nbt extra
        return stack;
    }
}
