package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Config.TierHandler;
import com.lypaka.battletower.Utils.DialogueTask;
import com.lypaka.battletower.Utils.FancyText;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentString;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.util.*;

// IMPORTANTE: quota
import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.limits.ChallengeQuotaService;

public final class TeamSelectionMenu {

    private static final Map<EntityPlayer, GooeyPage> PAGE_CACHE = new HashMap<>();
    private static final Map<EntityPlayer, List<Integer>> FILLED  = new HashMap<>();
    public  static final Map<EntityPlayer, Map<Integer, Pokemon>> CHOSEN = new HashMap<>();

    @SuppressWarnings("unused")
    public static void open(EntityPlayer player) throws ObjectMappingException {

        int maxParty = ConfigGetters.getMaxPartySize();
        int[] partySlots  = { 1,  2, 10, 11, 19, 20};
        int[] chosenSlots = computeChosenSlots(maxParty);
        Set<Integer> whiteSlots = computeWhiteSlots(chosenSlots);

        // TeamSelectionMenu.open()
        ChestTemplate template = ChestTemplate.builder(6).build();  // 6 × 9 = 54 slots

        GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(FancyText.getFancyString("Escolha seu time!"))
                .build();

        ItemStack glass = new ItemStack(Item.getByNameOrId("minecraft:stained_glass_pane"));
        GooeyButton white = GooeyButton.builder().display(glass).build();
        whiteSlots.forEach(s -> template.getSlot(s).setButton(white));

        List<EntityPixelmon> preTeam = YesNoMenu.playerTeam.get(player);
        String mode = BattleModeMenus.battleModeMap.get(player);

        for (int i = 0; i < preTeam.size(); i++) {

            EntityPixelmon p = preTeam.get(i);
            ItemStack sprite = ItemPixelmonSprite.getPhoto(p);
            NBTTagList lore  = new NBTTagList();

            boolean legal = validate(p, mode);

            sprite.setStackDisplayName(FancyText.getFancyString(
                    (legal ? "&a" : "&c") + p.getPokemonName()));

            if (legal) {
                lore.appendTag(new NBTTagString(
                        FancyText.getFancyString("&eLevel: " + p.getLvl().getLevel())));
                lore.appendTag(new NBTTagString(""));
                lore.appendTag(new NBTTagString(
                        FancyText.getFancyString("&4Click me to add to your challenge team!")));
            } else {
                lore.appendTag(new NBTTagString(
                        FancyText.getFancyString("&cThis Pokémon is blacklisted!")));
            }
            sprite.getOrCreateSubCompound("display").setTag("Lore", lore);

            int guiSlot = partySlots[i];
            if (legal) {
                int partyIndex = i;
                template.getSlot(guiSlot).setButton(GooeyButton.builder()
                        .display(sprite)
                        .onClick(a -> addPokemon(p, player, guiSlot, partyIndex, chosenSlots))
                        .build());
            } else {
                template.getSlot(guiSlot).setButton(GooeyButton.builder()
                        .display(sprite).build());
            }
        }

        ItemStack pokeBall = new ItemStack(Item.getByNameOrId("pixelmon:poke_ball"));
        GooeyButton emptyChoice = GooeyButton.builder().display(pokeBall).build();
        for (int slot : chosenSlots) template.getSlot(slot).setButton(emptyChoice);

        PAGE_CACHE.put(player, page);
        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

    private static void addPokemon(EntityPixelmon p, EntityPlayer player,
                                   int partyGuiSlot, int partyIndex, int[] chosenSlots) {

        FILLED   .computeIfAbsent(player, k -> new ArrayList<>()).add(partyGuiSlot);
        PartyStorage storage = Pixelmon.storageManager.getParty((EntityPlayerMP) player);
        Pokemon real = storage.get(partyIndex);           // pega o Pokémon verdadeiro

        CHOSEN.computeIfAbsent(player, k -> new HashMap<>())
            .put(partyIndex, real);

        GooeyPage page = PAGE_CACHE.get(player);

        int chosenPos = FILLED.get(player).size() - 1;
        ItemStack sprite = ItemPixelmonSprite.getPhoto(p);
        sprite.setStackDisplayName(FancyText.getFancyString("&e" + p.getPokemonName()));

        page.getTemplate().getSlot(chosenSlots[chosenPos])
                .setButton(GooeyButton.builder().display(sprite).build());

        ItemStack empty = new ItemStack(Item.getByNameOrId("pixelmon:poke_ball"));
        page.getTemplate().getSlot(partyGuiSlot)
                .setButton(GooeyButton.builder().display(empty).build());

        if (FILLED.get(player).size() == chosenSlots.length) {
            addConfirmAndReset(player, page);
        }
    }

    private static void addConfirmAndReset(EntityPlayer player, GooeyPage page) {

        List<String> tmp;
        try {
            tmp = ConfigManager
                    .getConfigNode(3, "NPC-Messages", "Challenge-Made")
                    .getList(TypeToken.of(String.class));
        } catch (ObjectMappingException e) {
            e.printStackTrace();
            tmp = Collections.singletonList("&c[Config error: Challenge-Made]");
        }
        final List<String> npcMsgs = tmp;

        ItemStack emerald = new ItemStack(Item.getByNameOrId("minecraft:emerald_block"));
        emerald.getOrCreateSubCompound("display")
               .setTag("Lore", lore("&eCONFIRM"));

        GooeyButton confirm = GooeyButton.builder()
            .display(emerald)
            .onClick(a -> {

                // ================== QUOTA: CONSUME AQUI (antes de iniciar) ==================
                ChallengeQuotaService quota = BattleTower.getQuota();
                if (quota != null) {
                    // Se souber o tier aqui, substitua null pelo tier real
                    ChallengeQuotaService.Result r = quota.consumeIfAllowed(player, null);
                    System.out.println("[BT-QuotaDebug] consumeIfAllowed: allowed=" + r.allowed + " remaining=" + r.remainingMillis);

                    if (!r.allowed) {
                        String base = ConfigManager
                                .getConfigNode(5, "Tower", "Challenge-Quota", "Message-When-Limited")
                                .getString("&cVocê já fez seu desafio. Volte em {time_left}.");
                        String msg = quota.blockedMessageWith(base, r.remainingMillis);
                        player.sendMessage(new TextComponentString(msg.replace("&", "§")));
                        return; // ABORTA início da challenge
                    }
                    // Observação: se esta foi a ÚLTIMA tentativa, o cooldown foi ligado dentro do consumeIfAllowed()
                }
                // ============================================================================

                DialogueTask.timer = new Timer();
                DialogueTask.timer.schedule(
                        new DialogueTask(player, 0, npcMsgs, "Challenge-Made"),
                        0, 1000);

                try {
                    TierHandler.startChallenge(
                            player,
                            CHOSEN,
                            BattleModeMenus.battleModeMap.get(player));
                } catch (info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException | IOException ex) {
                    ex.printStackTrace();
                    player.sendMessage(FancyText.getFancyComponent(
                            TextFormatting.RED + "Failed to start the challenge!"));
                }
            })
            .build();

        page.getTemplate().getSlot(4).setButton(confirm);

        ItemStack red = new ItemStack(Item.getByNameOrId("minecraft:redstone_block"));
        red.getOrCreateSubCompound("display").setTag("Lore", lore("&cRESET"));

        GooeyButton reset = GooeyButton.builder()
                .display(red)
                .onClick(a -> reset(player))
                .build();

        page.getTemplate().getSlot(22).setButton(reset);
    }

    private static void reset(EntityPlayer player) {
        FILLED.remove(player);
        CHOSEN.remove(player);
        UIManager.closeUI((EntityPlayerMP) player);

        new Timer().schedule(new TimerTask() {
            @Override public void run() { try { open(player); }
                                           catch (info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException e) { e.printStackTrace(); } }
        }, 500);
    }

    @SuppressWarnings("unlikely-arg-type")
    private static boolean validate(EntityPixelmon p, String mode) throws ObjectMappingException {

        if ("Level-50".equalsIgnoreCase(mode) && p.getLvl().getLevel() > 50) return false;

        List<String> blacklist = ConfigManager.getConfigNode(
                0, "Misc", "Pokemon-Blacklist").getList(TypeToken.of(String.class));

        if (p.getPokemonData().isEgg()) return false;

        if (blacklist.contains("legendaries")   && EnumSpecies.legendaries .contains(p.getPokemonName())) return false;
        if (blacklist.contains("ultra beasts")  && EnumSpecies.ultrabeasts.contains(p.getPokemonName())) return false;
        if (blacklist.contains("mythicals")     && isMythical(p.getPokemonName()))                      return false;

        return !blacklist.contains(p.getPokemonName());
    }

    private static boolean isMythical(String name) {
        final String[] mythicals = { "Mew","Celebi","Jirachi","Deoxys","Manaphy","Phione",
                "Darkrai","Shaymin","Arceus","Victini","Keldeo","Meloetta","Genesect",
                "Diancie","Hoopa","Volcanion","Magearna","Marshadow","Zeraora",
                "Meltan","Melmetal","Zarude"};
        for (String m : mythicals) if (m.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static NBTTagList lore(String line) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString(FancyText.getFancyString(line)));
        return list;
    }

    private static int[] computeChosenSlots(int max) {
        switch (max) {
            case 1:  return new int[]{15};
            case 2:  return new int[]{15, 16};
            case 3:  return new int[]{15, 16, 17};
            case 4:  return new int[]{15, 16, 17, 24};
            case 5:  return new int[]{15, 16, 17, 24, 25};
            default: return new int[]{15, 16, 17, 24, 25, 26};
        }
    }

    private static Set<Integer> computeWhiteSlots(int[] chosen) {

        Set<Integer> all = new HashSet<>();
        for (int i = 0; i < 27; i++) all.add(i);
        for (int i = 0; i < 27; i++) all.add(i + 27);

        int[] partySlots = {1, 2, 10, 11, 19, 20};
        for (int s : partySlots)  all.remove(s);
        for (int s : chosen)      all.remove(s);

        return all;
    }

    public static void clearSelection(EntityPlayer player) {
        FILLED.remove(player);
        CHOSEN.remove(player);
    }

    public static void removePage(EntityPlayer player) {
        PAGE_CACHE.remove(player);
    }
}
