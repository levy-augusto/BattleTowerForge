package com.lypaka.battletower.Utils;

import com.pixelmonmod.pixelmon.enums.EnumType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;

public class TMFactoryItemStack {

	private static final Map<EnumType, String> TM_MAP = new HashMap<>();

    private static final Map<EnumType, String> TR_MAP = new HashMap<>();

    static {
        // TMs (por tipo)
        TM_MAP.put(EnumType.Normal, "pixelmon:tm01");
        TM_MAP.put(EnumType.Fire, "pixelmon:tm35");
        TM_MAP.put(EnumType.Water, "pixelmon:tm55");
        TM_MAP.put(EnumType.Grass, "pixelmon:tm53");
        TM_MAP.put(EnumType.Electric, "pixelmon:tm24");
        TM_MAP.put(EnumType.Ice, "pixelmon:tm13");
        TM_MAP.put(EnumType.Fighting, "pixelmon:tm08");
        TM_MAP.put(EnumType.Poison, "pixelmon:tm36");
        TM_MAP.put(EnumType.Ground, "pixelmon:tm28");
        TM_MAP.put(EnumType.Flying, "pixelmon:tm40");
        TM_MAP.put(EnumType.Psychic, "pixelmon:tm29");
        TM_MAP.put(EnumType.Bug, "pixelmon:tm81");
        TM_MAP.put(EnumType.Rock, "pixelmon:tm80");
        TM_MAP.put(EnumType.Ghost, "pixelmon:tm30");
        TM_MAP.put(EnumType.Dragon, "pixelmon:tm02");
        TM_MAP.put(EnumType.Dark, "pixelmon:tm41");
        TM_MAP.put(EnumType.Steel, "pixelmon:tm91");
        TM_MAP.put(EnumType.Fairy, "pixelmon:tm99");

        // TRs (por tipo)
        TR_MAP.put(EnumType.Normal, "pixelmon:tr01");
        TR_MAP.put(EnumType.Fire, "pixelmon:tr02");
        TR_MAP.put(EnumType.Water, "pixelmon:tr03");
        TR_MAP.put(EnumType.Grass, "pixelmon:tr04");
        TR_MAP.put(EnumType.Electric, "pixelmon:tr05");
        TR_MAP.put(EnumType.Ice, "pixelmon:tr06");
        TR_MAP.put(EnumType.Fighting, "pixelmon:tr07");
        TR_MAP.put(EnumType.Poison, "pixelmon:tr08");
        TR_MAP.put(EnumType.Ground, "pixelmon:tr09");
        TR_MAP.put(EnumType.Flying, "pixelmon:tr10");
        TR_MAP.put(EnumType.Psychic, "pixelmon:tr11");
        TR_MAP.put(EnumType.Bug, "pixelmon:tr12");
        TR_MAP.put(EnumType.Rock, "pixelmon:tr13");
        TR_MAP.put(EnumType.Ghost, "pixelmon:tr14");
        TR_MAP.put(EnumType.Dragon, "pixelmon:tr15");
        TR_MAP.put(EnumType.Dark, "pixelmon:tr16");
        TR_MAP.put(EnumType.Steel, "pixelmon:tr17");
        TR_MAP.put(EnumType.Fairy, "pixelmon:tr18");
    }

    public static ItemStack getTMFactoryDisc(String move) {
        String discType = null;
        String EnumType = null;

        for (CustomDisc disc : CustomDisc.discs) {
            if (disc.getName().equalsIgnoreCase(move)) {
                discType = disc.getDiscType();
                EnumType = disc.getEnumType();
                break;
            }
        }

        if (discType == null || EnumType == null) {
            ItemStack stack = new ItemStack(Item.getByNameOrId("minecraft:barrier"));
            stack.setStackDisplayName(FancyText.getFancyString("&cDisc does not exist!"));
            return stack;
        }

        discType = normalizeDiscType(discType);
        ItemStack disc = getDisc(discType, EnumType);
        disc.setStackDisplayName(TextFormatting.WHITE + move);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("HideTooltip", true);
        disc.setTagCompound(tag);

        return disc;
    }

    private static String normalizeDiscType(String type) {
        switch (type.toLowerCase()) {
            case "tm":
            case "tms":
                return "TMs";
            case "tr":
            case "trs":
                return "TRs";
            default:
                return "Unknown";
        }
    }

    private static ItemStack getDisc(String discType, String typeName) {
    EnumType moveType;
    try {
        moveType = EnumType.valueOf(typeName.toUpperCase());  
    } catch (IllegalArgumentException ex) {                  
        return ItemStack.EMPTY;
    }

    String itemId = null;
    if ("TMs".equals(discType)) {
        itemId = TM_MAP.get(moveType);
    } else if ("TRs".equals(discType)) {
        itemId = TR_MAP.get(moveType);
    }

    return itemId != null ? new ItemStack(Item.getByNameOrId(itemId))
                          : ItemStack.EMPTY;
    }

}
