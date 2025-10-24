package com.lypaka.battletower.Guis;

import com.codehusky.huskyui.StateContainer;
import com.codehusky.huskyui.states.Page;
import com.codehusky.huskyui.states.element.Element;
import com.codehusky.huskyui.states.element.ActionableElement;
import com.codehusky.huskyui.states.action.ActionType;
import com.codehusky.huskyui.states.action.runnable.RunnableAction;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.TMFactory;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.ItemCreator;
import com.lypaka.battletower.Utils.ItemStackGetter;

import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * GUI de compra para TM / TR / HM.
 */
public class ShopGUI {

    /* ------------------------------------------------------------ */
    /*  Página principal                                            */
    /* ------------------------------------------------------------ */
    public static void openShopPage(Player player, String disc) throws ObjectMappingException {

        /* container & página */
        StateContainer container = new StateContainer();
        Page main = Page.builder()
                .setAutoPaging(true)
                .setInventoryDimension(InventoryDimension.of(9, 6))
                .setTitle(Text.of(TextColors.YELLOW, disc + " Shop"))
                .setEmptyStack(EmptyPage.empty())
                .build("main");

        /* borda vazia */
        int[] borderSlots = {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35,
                36, 37, 38, 39, 40, 41, 42, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53
        };
        for (int slot : borderSlots) {
            main.getElements().put(slot, new Element(EmptyPage.empty()));
        }

        /* itens da loja */
        Map<String, Integer> shopMap = ConfigManager
                .getConfigNode(4, disc + "-Shop")
                .getValue(new ShopTypeToken());

        int slotIndex = 0;
        for (Map.Entry<String, Integer> entry : shopMap.entrySet()) {

            ItemStack icon = ItemStack.builder()
                    .fromItemStack(ItemStackGetter.getDisc(disc, entry.getKey()))
                    .add(Keys.ITEM_LORE, Lists.newArrayList(
                            Text.of("Price: " + entry.getValue()),
                            Text.of("Click me to buy!")
                    ))
                    .build();

            RunnableAction action = new RunnableAction(
                    container,
                    ActionType.NORMAL,
                    "main",
                    c -> buyDisc(disc, entry.getKey(), entry.getValue(), player)
            );

            main.getElements().put(slotIndex++, new ActionableElement(action, icon));
        }

        container.setInitialState(main);
        container.launchFor(player);
    }

    /* ------------------------------------------------------------ */
    /*  Compra                                                      */
    /* ------------------------------------------------------------ */
    private static void buyDisc(String disc, String move, int price, Player player) {

        Optional<EconomyService> econOpt = Sponge.getServiceManager().provide(EconomyService.class);
        if (!econOpt.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "Economy não disponível."));
            return;
        }

        EconomyService econ = econOpt.get();
        Currency cur = econ.getDefaultCurrency();
        Optional<UniqueAccount> accOpt = econ.getOrCreateAccount(player.getUniqueId());

        if (!accOpt.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "Conta econômica não encontrada."));
            return;
        }

        UniqueAccount account = accOpt.get();
        if (account.getBalance(cur).intValue() < price) {
            player.sendMessage(Text.of(TextColors.RED, "Not enough money!"));
            return;
        }

        /* cobra e entrega */
        EventContext ctx = EventContext.builder()
                .add(EventContextKeys.PLUGIN, TMFactory.getContainer())
                .build();
        account.withdraw(cur, BigDecimal.valueOf(price), Cause.of(ctx, TMFactory.getContainer()));

        switch (disc.toUpperCase()) {
            case "TM": ItemCreator.giveTM(player, move, 1); break;
            case "TR": ItemCreator.giveTR(player, move, 1); break;
            case "HM": ItemCreator.giveHM(player, move, 1); break;
        }

        player.sendMessage(Text.of(TextColors.GREEN,
                "You bought a " + disc + " for move " + move + "!"));
    }

    /* ------------------------------------------------------------ */
    /*  Helper TypeToken                                            */
    /* ------------------------------------------------------------ */
    private static class ShopTypeToken extends TypeToken<Map<String, Integer>> {
        private static final long serialVersionUID = 1L;
    }
}
