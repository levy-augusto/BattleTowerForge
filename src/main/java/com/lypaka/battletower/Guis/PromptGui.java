package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;

import java.io.IOException;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Config.TierHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class PromptGui {

    public static void showPromptMenu(EntityPlayer player, boolean canPause) {
        ChestTemplate template = ChestTemplate.builder(3).build();
        GooeyPage page = GooeyPage.builder().template(template).title(FancyText.getFancyString("&4Continue?")).build();

        int[] red = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        int[] black = {9, 10, 12, 13, 14, 16, 17};
        int[] white = {18, 19, 20, 21, 22, 23, 24, 25, 26};

        setColoredSlots(page, red, "red");
        setColoredSlots(page, black, "black");
        setColoredSlots(page, white, "white");

        setupYesButton(page, 11, player);
        if (canPause) setupPauseButton(page, 13, player);
        setupNoButton(page, 15, player);

        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

    private static void setColoredSlots(GooeyPage page, int[] slots, String color) {
        for (int i : slots) {
            page.getTemplate().getSlot(i).setButton(getGlassButton(color));
        }
    }

    private static void setupYesButton(GooeyPage page, int slot, EntityPlayer player) {
        ItemStack yesButton = new ItemStack(Item.getByNameOrId("minecraft:emerald_block"));
        yesButton.setStackDisplayName(FancyText.getFancyString("&aYES"));
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString(FancyText.getFancyString("&eClick me to move on to the next Trainer!")));
        yesButton.getOrCreateSubCompound("display").setTag("Lore", lore);

        Button button = GooeyButton.builder()
                .display(yesButton)
                .onClick(action -> {
                    try {
                        TierHandler.nextChallenge(player);
                    } catch (ObjectMappingException e) {
                        e.printStackTrace();
                    }
                }).build();

        page.getTemplate().getSlot(slot).setButton(button);
    }

    private static void setupPauseButton(GooeyPage page, int slot, EntityPlayer player) {
        ItemStack pauseButton = new ItemStack(Item.getByNameOrId("minecraft:gold_block"));
        pauseButton.setStackDisplayName(FancyText.getFancyString("&ePAUSE"));
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString(FancyText.getFancyString("&eClick me to pause your challenge!")));
        pauseButton.getOrCreateSubCompound("display").setTag("Lore", lore);

        Button button = GooeyButton.builder()
                .display(pauseButton)
                .onClick(action -> {
                    try {
                        TierHandler.pauseChallenge(player);
                    } catch (ObjectMappingException e) {
                        e.printStackTrace();
                    }
                }).build();

        page.getTemplate().getSlot(slot).setButton(button);
    }

    private static void setupNoButton(GooeyPage page, int slot, EntityPlayer player) {
        ItemStack noButton = new ItemStack(Item.getByNameOrId("minecraft:redstone_block"));
        noButton.setStackDisplayName(FancyText.getFancyString("&cNO"));
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString(FancyText.getFancyString("&eClick me to end your challenge!")));
        noButton.getOrCreateSubCompound("display").setTag("Lore", lore);

        Button button = GooeyButton.builder()
                .display(noButton)
                .onClick(action -> {
                    try {
                        TierHandler.cancelChallenge(player);
                        try {
                            ConfigManager.savePlayerConfig(player.getUniqueID());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } catch (ObjectMappingException e) {
                        e.printStackTrace();
                    }
                }).build();

        page.getTemplate().getSlot(slot).setButton(button);
    }

    private static Button getGlassButton(String color) {
    	int metadata;
    	switch (color.toLowerCase()) {
    	    case "red":
    	        metadata = 14;
    	        break;
    	    case "black":
    	        metadata = 15;
    	        break;
    	    default:
    	        metadata = 0;
    	        break;
    	}

        ItemStack glass = new ItemStack(Item.getByNameOrId("minecraft:stained_glass_pane"));
        glass.setItemDamage(metadata);

        return GooeyButton.builder().display(glass).build();
    }
}
