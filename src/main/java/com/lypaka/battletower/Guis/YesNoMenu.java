package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.lypaka.battletower.Utils.FancyText;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;

public final class YesNoMenu {

    private YesNoMenu() {}

    public static final Map<EntityPlayer, List<EntityPixelmon>> playerTeam = new HashMap<>();

    private static ItemStack pane(int meta, String displayName) {
        ItemStack stack = new ItemStack(Blocks.STAINED_GLASS_PANE, 1, meta);
        stack.setStackDisplayName(displayName);

        NBTTagCompound tag = stack.getOrCreateSubCompound("display");
        tag.setBoolean("HideFlags", true);      
        return stack;
    }


    private static final GooeyButton FILLER =
        GooeyButton.builder()
                   .display(GuiUtils.pane(15)) // preto
                   .build();

    public static void openYesNoContainer(EntityPlayer player) throws ObjectMappingException {

        ChestTemplate template = ChestTemplate.builder(1).build();   
        GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(FancyText.getFancyString("Desafiar a Battle Tower?"))
                .build();

        page.getTemplate().getSlot(2).setButton(
                GooeyButton.builder()
                        .display(pane(5, "Sim"))          
                        .onClick(click -> {
                            EntityPlayerMP p = (EntityPlayerMP) click.getPlayer();

                            // Copia a party do jogador
                            List<EntityPixelmon> team = getPartyAsEntities(p);
                            YesNoMenu.playerTeam.put(p, team);
                             p.closeScreen();

                             p.sendMessage(FancyText.getFancyComponent("Escolha o modo de Batalha Desejado."));
                             try {
                                BattleModeMenus.openBattleModeSelectionMenu(p);
                            } catch (ObjectMappingException e) {
                                e.printStackTrace();
                            }
                           
                        })
                        .build()
        );

        page.getTemplate().getSlot(6).setButton(
                GooeyButton.builder()
                        .display(pane(14, "Nao"))          
                        .onClick(click -> player.closeScreen())
                        .build()
        );

        for (int i = 0; i < 9; i++) {
            if (page.getTemplate().getSlot(i).getButton() == null) {
                page.getTemplate().getSlot(i).setButton(FILLER);
            }
        }

        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

        private static List<EntityPixelmon> getPartyAsEntities(EntityPlayerMP player) {

            PartyStorage storage = Pixelmon.storageManager.getParty(player);

            List<EntityPixelmon> list = new ArrayList<>();
            // percorre apenas os Pokémon existentes
            for (com.pixelmonmod.pixelmon.api.pokemon.Pokemon poke : storage.getTeam()) {
                EntityPixelmon ep = new EntityPixelmon(player.world); // entidade temporária
                ep.setPokemon(poke);
                list.add(ep);
            }
            return list;
        }

}
