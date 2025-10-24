package com.lypaka.battletower.Guis;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.DialogueTask;
import com.lypaka.battletower.Utils.FancyText;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.*;

/**
 * Gera o menu para seleção de modos de batalha.
 */
public class BattleModeMenus {

    public static final Map<EntityPlayer, String> battleModeMap = new HashMap<>();

    /** Ordem fixa dos modos que aparecem nos slots {11, 13, 15}. */
    private static final String[] MODES = {"Level-50", "Level-100", "Open-Level"};

    /* --------------------------------------------------------------------- */
    /*  API pública                                                          */
    /* --------------------------------------------------------------------- */

    /** Abre a GUI principal de escolha de modos. */
    public static void openBattleModeSelectionMenu(EntityPlayer player) throws ObjectMappingException {

        ChestTemplate template = ChestTemplate.builder(3).build();
        GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(FancyText.getFancyString("Selecione o Modo de Batalha!"))
                .build();

        // molduras de vidro
        int[] red   = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        int[] black = {9, 10, 12, 14, 16, 17};
        int[] white = {18, 19, 20, 21, 22, 23, 24, 25, 26};
        for (int slot : red)   template.getSlot(slot).setButton(getGlassButton("red"));
        for (int slot : black) template.getSlot(slot).setButton(getGlassButton("black"));
        for (int slot : white) template.getSlot(slot).setButton(getGlassButton("white"));

        // estrelas (11, 13, 15) → índices 0,1,2 em MODES
        int[] starSlots = {11, 13, 15};
        for (int i = 0; i < starSlots.length; i++) {
            template.getSlot(starSlots[i]).setButton(buildBattleModeButton(i));
        }

        UIManager.openUIForcefully((EntityPlayerMP) player, page);
    }

    /* --------------------------------------------------------------------- */
    /*  Construção dos botões                                                */
    /* --------------------------------------------------------------------- */

    /** Constrói o botão da estrela para o modo indicado pelo índice. */
    private static GooeyButton buildBattleModeButton(int index) throws ObjectMappingException {
        String mode = MODES[index];

        if (isBattleModeEnabled(mode)) {
            return buildEnabledButton(mode);
        }
        return buildDisabledButton();
    }

    /* ------------------------ Funções auxiliares ------------------------- */

    private static boolean isBattleModeEnabled(String mode) throws ObjectMappingException {
        return ConfigManager
                .getConfigNode(0, "Battle-Modes", mode)
                .getBoolean();
    }

    private static ItemStack createStarItem(String mode) {
        ItemStack star = new ItemStack(Item.getByNameOrId("minecraft:nether_star"));
        star.setStackDisplayName(FancyText.getFancyString("&4" + mode));
        return star;
    }

    private static ItemStack createBarrierItem() {
        ItemStack barrier = new ItemStack(Item.getByNameOrId("minecraft:barrier"));
        barrier.setStackDisplayName(FancyText.getFancyString("&cBattle Mode disabled!"));
        return barrier;
    }

    private static GooeyButton buildEnabledButton(String mode) throws ObjectMappingException {

        List<String> messages = ConfigManager
                .getConfigNode(3, "NPC-Messages", "Team-Selection")
                .getList(TypeToken.of(String.class));

        return GooeyButton.builder()
                .display(createStarItem(mode))
                .onClick(action -> {
                    UIManager.closeUI(action.getPlayer());
                    battleModeMap.put(action.getPlayer(), mode);
                    DialogueTask.timer = new Timer();
                    DialogueTask.timer.schedule(
                            new DialogueTask(action.getPlayer(), 0, messages, "Team-Selection"),
                            0L, 1000L
                    );
                })
                .build();
    }

    private static GooeyButton buildDisabledButton() {
        return GooeyButton.builder()
                .display(createBarrierItem())
                .build();
    }

    /* ------------------ vidro decorativo -------------------------------- */

    private static Button getGlassButton(String color) {
        int meta;
        switch (color.toLowerCase()) {
            case "red":   meta = 14; break;
            case "black": meta = 15; break;
            default:      meta = 0;  break; // white
        }
            return GooeyButton.builder()
                      .display(GuiUtils.pane(meta)) // nome em branco!
                      .build();
    }
}
