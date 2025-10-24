package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Utils.TMFactoryItemStack;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;

import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** UI de loja estilo Shopkeeper (grade + comprar/cancelar, sem abas nem quantidade). */
public class ShopkeeperLikeUI {

    // ---- Estado por jogador
    private static final Map<UUID, ShopState> STATES = new ConcurrentHashMap<>();

    private static class ShopState {
        UUID player;
        String shopId;
        int page = 0;
        int selectedIndex = -1;
        List<ShopEntry> buy = new ArrayList<>();
        String title = "BP Shop";
    }

    private static class ShopEntry {
        String id;
        String name;
        int meta;
        int price;
        ItemStack icon; // cache do display

        ShopEntry(String id, String name, int meta, int price) {
            this.id = id;
            this.name = name;
            this.meta = meta;
            this.price = price;
        }
    }

    /** Abre a loja por ID. */
    public static void open(EntityPlayer player, String shopId) throws ObjectMappingException {
        ConfigurationNode root = ConfigManager.getConfigNode(1, new Object[]{"Shops", shopId});
        if (root.isVirtual()) {
            player.sendMessage(FancyText.getFancyComponent("&cLoja &e" + shopId + " &cnão encontrada em shops.conf!"));
            return;
        }

        ShopState st = new ShopState();
        st.player = player.getUniqueID();
        st.shopId = shopId;
        st.title = root.getNode("Title").getString(shopId + " BP Shop");

        // Preferencial: seção Buy no config; compat: Slot-* na raiz

    if (!root.getNode("Buy").isVirtual()) {
        st.buy = readSection(root.getNode("Buy"), player);
    } else {
        Map<String, ConfigurationNode> slots = root.getChildrenMap().entrySet().stream()
                .filter(e -> String.valueOf(e.getKey()).startsWith("Slot-"))
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));

        for (ConfigurationNode n : slots.values()) {
            String id = n.getNode("ID").getString("");
            if (id.isEmpty()) continue;
            String name = n.getNode("Display-Name").getString("Item");
            int meta = n.getNode("Metadata").getInt(0);
            int price = n.getNode("Price").getInt(-1);
            if (price < 0) continue;

            ShopEntry e = new ShopEntry(id, name, meta, price);

            // >>> CRUCIAL: gerar o ícone (antes ficava null e virava paper)
            e.icon = getDisplayIcon(e.id, player, e.meta);
            if (e.name != null && !e.name.isEmpty()) {
                e.icon.setStackDisplayName(FancyText.getFancyString(e.name));
            }

            // (opcional) log pra confirmar
            System.out.println("[BT-ShopDebug] Slot-compat add: id='" + e.id + "' name='" + e.name + "' meta=" + e.meta + " price=" + e.price);

            st.buy.add(e);
        }
    }


        STATES.put(st.player, st);
        GooeyPage page = buildPage((EntityPlayerMP) player, st);
        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

    private static List<ShopEntry> readSection(ConfigurationNode section, EntityPlayer player) throws ObjectMappingException {
        if (section.isVirtual()) return new ArrayList<>();
        
        List<ShopEntry> list = new ArrayList<>();
        if (!section.getChildrenMap().isEmpty()) {
            for (ConfigurationNode n : section.getChildrenMap().values()) {
                String id = n.getNode("ID").getString("");
                if (id.isEmpty()) continue;
                String name = n.getNode("Display-Name").getString("Item");
                int meta = n.getNode("Metadata").getInt(0);
                int price = n.getNode("Price").getInt(-1);
                if (price < 0) continue;
                list.add(new ShopEntry(id, name, meta, price));
                list.add(new ShopEntry(id, name, meta, price));
System.out.println("[BT-ShopDebug] Lido do config: id='" + id + "' name='" + name + "' meta=" + meta + " price=" + price);

            }
        } else {
            List<Map<String, Object>> raw = section.getList(new TypeToken<Map<String, Object>>() {});
            for (Map<String, Object> map : raw) {
                String id = String.valueOf(map.getOrDefault("ID", ""));
                if (id.isEmpty()) continue;
                String name = String.valueOf(map.getOrDefault("Display-Name", "Item"));
                int meta = asInt(map.getOrDefault("Metadata", 0));
                int price = asInt(map.getOrDefault("Price", -1));
                if (price < 0) continue;
                list.add(new ShopEntry(id, name, meta, price));
                list.add(new ShopEntry(id, name, meta, price));
System.out.println("[BT-ShopDebug] Lido do config: id='" + id + "' name='" + name + "' meta=" + meta + " price=" + price);

            }
        }
        
        for (ShopEntry e : list) {
            e.icon = getDisplayIcon(e.id, player, e.meta);
            
            if (e.name != null && !e.name.isEmpty()) {
                e.icon.setStackDisplayName(FancyText.getFancyString(e.name));
            }
        }
        return list;
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private static GooeyPage buildPage(EntityPlayerMP player, ShopState st) {
        final int rows = 6;
        final int totalSlots = rows * 9;

        ChestTemplate template = ChestTemplate.builder(rows).build();
        GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(FancyText.getFancyString("&4" + st.title))
                .build();

        // ---- Grade de itens (7x3: 10-16, 19-25, 28-34)
        List<ShopEntry> entries = (st.buy != null ? st.buy : Collections.emptyList());
        final int pageSize = 21;
        int start = st.page * pageSize;
        int end = Math.min(start + pageSize, entries.size());

        int[] grid = {0,1,2,3,4,5,6,7,8,9, 10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34};

        for (int i = start, gi = 0; i < end && gi < grid.length; i++, gi++) {
            int slot = grid[gi];
            int idx = i;
            ShopEntry e = entries.get(idx);

            

            ItemStack rawIcon = (e.icon == null || e.icon == ItemStack.EMPTY)
                    ? new ItemStack(Item.getByNameOrId("minecraft:paper"))
                    : e.icon;

            ItemStack display = rawIcon.copy();
            List<String> lore = new ArrayList<>();
            lore.add("&7Preço: &e" + e.price + " BP");
            setLore(display, lore);

            if (slot >= 0 && slot < totalSlots && template.getSlot(slot) != null) {

    System.out.println("[BT-ShopDebug] Slot " + slot + " exibindo id='" + e.id + "' -> "
        + (display.getItem() != null && display.getItem().getRegistryName() != null
           ? display.getItem().getRegistryName().toString()
           : "null"));


                template.getSlot(slot).setButton(
                        GooeyButton.builder()
                                .display(display)
                                .onClick(a -> {
                                    st.selectedIndex = idx;
                                    reopen(player, st);
                                })
                                .build()
                );
            }
        }

        // ---- Controles de página (linha 4: 36..44)
        template.getSlot(46).setButton(navButton("Anterior", Items.ARROW, () -> {
            if (st.page > 0) st.page--;
            st.selectedIndex = -1;
            reopen(player, st);
        }, st.page == 0));

        template.getSlot(49).setButton(labelButton("Página " + (st.page + 1), Items.PAPER));

        boolean hasNext = (start + pageSize) < entries.size();
        template.getSlot(52).setButton(navButton("Próximo", Items.ARROW, () -> {
            if (hasNext) st.page++;
            st.selectedIndex = -1;
            reopen(player, st);
        }, !hasNext));

        // ---- Barra inferior (apenas Comprar e Cancelar lado a lado)
        // Coloca Comprar (esmeralda) no slot 50 e Cancelar (barreira) no slot 51
        template.getSlot(53).setButton(confirmButton(player, st));
        template.getSlot(45
        
        
        ).setButton(cancelButton());

        return page;
    }

    // ---- Botões auxiliares

    private static GooeyButton navButton(String text, Item itemIcon, Runnable action, boolean disabled) {
        ItemStack icon = new ItemStack(itemIcon);
        icon.setStackDisplayName(FancyText.getFancyString(disabled ? "&7" + text : "&f" + text));
        return GooeyButton.builder()
                .display(icon)
                .onClick(a -> { if (!disabled) action.run(); })
                .build();
    }

    private static GooeyButton labelButton(String text, Item iconItem) {
        if (iconItem == null) iconItem = Item.getByNameOrId("minecraft:paper");
        ItemStack icon = new ItemStack(iconItem);
        icon.setStackDisplayName(FancyText.getFancyString("&f" + text));
        return GooeyButton.builder().display(icon).build();
    }

    private static GooeyButton cancelButton() {
        ItemStack barrier = new ItemStack(Item.getByNameOrId("minecraft:barrier"));
        barrier.setStackDisplayName(FancyText.getFancyString("&cCancelar"));
        return GooeyButton.builder()
                .display(barrier)
                .onClick(a -> a.getPlayer().closeScreen())
                .build();
    }

    private static GooeyButton confirmButton(EntityPlayerMP player, ShopState st) {
        ItemStack emerald = new ItemStack(Items.EMERALD);
        emerald.setStackDisplayName(FancyText.getFancyString("&aComprar"));
        List<String> lore = new ArrayList<>();
        if (st.selectedIndex >= 0) {
            ShopEntry e = st.buy.get(st.selectedIndex);
            lore.add("&7Item: &f" + e.name);
            lore.add("&7Preço: &e" + e.price + " BP");
        } else {
            lore.add("&7Selecione um item.");
        }
        setLore(emerald, lore);

        return GooeyButton.builder()
                .display(emerald)
                .onClick(a -> {
                    if (st.selectedIndex < 0) return;
                    ShopEntry e = st.buy.get(st.selectedIndex);

                    int bp = ConfigGetters.getCurrentBP(a.getPlayer());
                    if (bp < e.price) {
                        a.getPlayer().sendMessage(FancyText.getFancyComponent("&cBP insuficiente (&e" + bp + "&c / &e" + e.price + "&c)."));
                        return;
                    }
                    try {
                        // compra de 1 unidade
                        BPShops.exchangeBP(a.getPlayer(), e.price, e.id, e.name);
                    } catch (ObjectMappingException ex) {
                        ex.printStackTrace();
                    }
                    // limpa seleção e reabre
                    st.selectedIndex = -1;
                    reopen(player, st);
                })
                .build();
    }

    private static void reopen(EntityPlayerMP player, ShopState st) {
        GooeyPage page = buildPage(player, st);
        UIManager.openUIForcefully(player, page);
    }

    // ---- Helpers visuais e de item

    private static void setLore(ItemStack stack, List<String> lines) {
        if (stack == null || stack == ItemStack.EMPTY || lines == null || lines.isEmpty()) return;

        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new net.minecraft.nbt.NBTTagCompound();
            stack.setTagCompound(tag);
        }
        net.minecraft.nbt.NBTTagCompound display = tag.getCompoundTag("display");
        tag.setTag("display", display);

        net.minecraft.nbt.NBTTagList loreTag = new net.minecraft.nbt.NBTTagList();
        for (String l : lines) {
            loreTag.appendTag(new net.minecraft.nbt.NBTTagString(FancyText.getFancyString(l)));
        }
        display.setTag("Lore", loreTag);
    }

    /** Ícone do item (TM/booster/Foto Pokémon/vanilla), com fallback em PAPER. */
/** Ícone do item (TM/booster/Foto Pokémon/vanilla), com fallback em PAPER + debugs detalhados. */
private static ItemStack getDisplayIcon(String entry, EntityPlayer player, int meta) {
    String raw = entry == null ? "" : entry.trim();
    System.out.println("[BT-ShopDebug] getDisplayIcon entry='" + raw + "' meta=" + meta);
    try {
        String[] split = raw.split(":");
        String type = split[0].toLowerCase();

        switch (type) {
            case "customtm":
                return TMFactoryItemStack.getTMFactoryDisc(split[1]);
            case "booster":
                return com.lypaka.battletower.Handlers.BoosterHandler.getBoosterItem(split[1]);
            case "cratekey":
                return new ItemStack(Item.getByNameOrId("minecraft:nether_star"));
            case "pokemon": {
                EntityPixelmon pkmn = PokemonSpec.from(split[1]).create(player.world);
                return ItemPixelmonSprite.getPhoto(pkmn);
            }
            default: {
                Item item = resolveItemStrict(raw);
                if (item == null && !raw.contains(":")) {
                    // Sem namespace -> tenta pixelmon:
                    item = resolveItemStrict("pixelmon:" + raw);
                }
                if (item == null && raw.startsWith("pixelmon:") && !raw.endsWith("_item")) {
                    // Fallback comum em alguns nomes
                    item = resolveItemStrict(raw + "_item");
                }
                if (item == null) {
                    System.out.println("[BT-ShopDebug] NENHUM item encontrado p/ '" + raw + "' (usando PAPER)");
                    return new ItemStack(Item.getByNameOrId("minecraft:paper"));
                }
                ItemStack stack = new ItemStack(item);
                if (meta != 0) stack.setItemDamage(meta);
                return stack;
            }
        }
    } catch (Throwable t) {
        System.out.println("[BT-ShopDebug] Erro em getDisplayIcon para '" + entry + "': " + t);
        return new ItemStack(Item.getByNameOrId("minecraft:paper"));
    }
}

/** Tenta resolver via getByNameOrId e ForgeRegistries, com logs. */
private static Item resolveItemStrict(String id) {
    try {
        String norm = id == null ? "" : id.trim();
        System.out.println("[BT-ShopDebug] resolveItemStrict: tentando '" + norm + "'");
        Item byName = Item.getByNameOrId(norm);
        if (byName != null) {
            System.out.println("[BT-ShopDebug]  -> getByNameOrId OK: " + byName.getRegistryName());
            return byName;
        }
        // Forge registry
        ResourceLocation rl = new ResourceLocation(norm);
        Item byForge = ForgeRegistries.ITEMS.getValue(rl);
        System.out.println("[BT-ShopDebug]  -> ForgeRegistries: " + (byForge != null ? byForge.getRegistryName() : "null"));
        return byForge;
    } catch (Exception e) {
        System.out.println("[BT-ShopDebug]  -> exception em resolveItemStrict('" + id + "'): " + e);
        return null;
    }
}

}
