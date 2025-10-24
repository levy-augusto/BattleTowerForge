package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Utils.TMFactoryItemStack;
import com.lypaka.battletower.Handlers.BoosterHandler;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BPShops {

    /** Abre uma loja por ID, ex.: "1-BP" */
    public static void openShop(EntityPlayer player, String shopId) throws ObjectMappingException {
        ConfigurationNode shopRoot = ConfigManager.getConfigNode(1, new Object[]{"Shops", shopId});
        if (shopRoot.isVirtual()) {
            player.sendMessage(FancyText.getFancyComponent("&cLoja &e" + shopId + " &cnão encontrada em shops.conf!"));
            return;
        }

        int rows = Math.max(1, Math.min(6, shopRoot.getNode("Rows").getInt(6)));
        String title = shopRoot.getNode("Title").getString(shopId + " BP Shop");

        ChestTemplate template = ChestTemplate.builder(rows).build();
        GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(FancyText.getFancyString("&4" + title))
                .build();

        // Coleta todas as chaves Slot-*
        Map<String, ConfigurationNode> slotNodes = shopRoot.getChildrenMap().entrySet().stream()
                .filter(e -> String.valueOf(e.getKey()).startsWith("Slot-"))
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));

        for (Map.Entry<String, ConfigurationNode> entry : slotNodes.entrySet()) {
            String key = entry.getKey();              // ex.: Slot-12
            ConfigurationNode node = entry.getValue();

            int slotIndex = parseSlotIndex(key);
            if (slotIndex < 0 || slotIndex >= rows * 9) continue;

            String id   = node.getNode("ID").getString("");
            String name = node.getNode("Display-Name").getString("Item");
            int meta    = node.getNode("Metadata").getInt(0);
            int price   = node.getNode("Price").getInt(-1);

            if (id.isEmpty() || price < 0) continue;

            ItemStack icon = getDisplayIcon(id, player, meta);
            icon.setStackDisplayName(FancyText.getFancyString(name));

            // Lore opcional
            List<String> lore = new ArrayList<>();
            if (!node.getNode("Lore").isVirtual()) {
                try {
                    lore = node.getNode("Lore").getList(TypeToken.of(String.class));
                } catch (ObjectMappingException ignored) {}
            }
            // Sempre inclui o preço ao final
            lore.add("&ePreço: " + price + " BP");

            

            GooeyButton button = GooeyButton.builder()
                    .display(icon)
                    .onClick(action -> {
                        try {
                            exchangeBP(player, price, id, name);
                        } catch (ObjectMappingException e) {
                            e.printStackTrace();
                        }
                    })
                    .build();

            page.getTemplate().getSlot(slotIndex).setButton(button);
        }

        // Slots não configurados ficam em air (já é o padrão)
        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

    private static int parseSlotIndex(String slotKey) {
        try {
            return Integer.parseInt(slotKey.replace("Slot-", "").trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static ItemStack getDisplayIcon(String entry, EntityPlayer player, int meta) throws ObjectMappingException {
        String[] split = entry.split(":");
        String type = split[0];

        switch (type.toLowerCase()) {
            case "customtm":
                return TMFactoryItemStack.getTMFactoryDisc(split[1]);
            case "booster":
                return BoosterHandler.getBoosterItem(split[1]);
            case "cratekey":
                return new ItemStack(Item.getByNameOrId("minecraft:nether_star"));
            case "pokemon":
                EntityPixelmon pokemon = PokemonSpec.from(split[1]).create(player.world);
                return ItemPixelmonSprite.getPhoto(pokemon);
            default:
                ItemStack stack = new ItemStack(Item.getByNameOrId(entry));
                if (meta != 0) stack.setItemDamage(meta);
                return stack;
        }
    }

    public static void exchangeBP(EntityPlayer player, int price, String id, String displayName) throws ObjectMappingException {
        if (ConfigGetters.getCurrentBP(player) < price) {
            player.sendMessage(FancyText.getFancyComponent("&cBP insuficiente para comprar isso!"));
            return;
        }

        String[] split = id.split(":");
        String type = getItemType(id);
        String keyName = split.length > 1 ? split[1] : "";

        switch (type) {
            case "item": {
                ItemStack item = new ItemStack(Item.getByNameOrId(id));
                item.setCount(1);
                player.inventory.addItemStackToInventory(item);
                break;
            }
            case "booster": {
                ItemStack booster = BoosterHandler.getBoosterItem(keyName);
                booster.setCount(1);
                player.inventory.addItemStackToInventory(booster);
                break;
            }
            case "tm": {
                ItemStack tm = TMFactoryItemStack.getTMFactoryDisc(keyName);
                tm.setCount(1);
                player.inventory.addItemStackToInventory(tm);
                break;
            }
            case "pokemon": {
                // Cria o Pokémon (API Reforged 8.4.3)
                com.pixelmonmod.pixelmon.api.pokemon.Pokemon poke = PokemonSpec.from(keyName).create();
                PartyStorage storage = Pixelmon.storageManager.getParty((EntityPlayerMP) player);
                if (storage != null) storage.add(poke);
                break;
            }
            case "cratekey": {
                if (BattleTower.isCratesSystemLoaded) {
                    String command = com.lypaka.battletower.Config.ConfigGetters.getCratesCommand()
                            .replace("%keyName%", keyName)
                            .replace("%player%", player.getName());
                    BattleTower.server.getCommandManager().executeCommand(BattleTower.server, command);
                } else {
                    player.sendMessage(FancyText.getFancyComponent("&cDependência HuskyCrates ausente!"));
                    return;
                }
                break;
            }
        }

        ConfigGetters.setBP(player, ConfigGetters.getCurrentBP(player) - price);
        try {
            ConfigManager.savePlayerConfig(player.getUniqueID());
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendMessage(FancyText.getFancyComponent("&aVocê comprou &e" + displayName + " &apor &e" + price + " BP&a!"));
    }

    private static String getItemType(String id) {
        String type = id.split(":")[0].toLowerCase();
        switch (type) {
            case "booster":  return "booster";
            case "pokemon":  return "pokemon";
            case "customtm": return "tm";
            case "cratekey": return "cratekey";
            default:         return "item";
        }
    }
}
