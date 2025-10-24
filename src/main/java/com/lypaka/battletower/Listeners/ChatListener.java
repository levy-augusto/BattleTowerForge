package com.lypaka.battletower.Listeners;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.TMFactory;
import com.lypaka.battletower.Commands.PlayerCommands;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.FancyText;
import com.lypaka.battletower.Utils.SelfCancellingTask;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent.Chat;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatListener {

    public static String move;

    @Listener
    public void onChat(Chat event, @Root Player player) throws Exception {

        /* ------------------------------------------------------------------ */
        /* Apenas se o jogador estiver no fluxo de solicitação de movimento   */
        /* ------------------------------------------------------------------ */
        if (PlayerCommands.playerRequestingMove == null
                || PlayerCommands.playerRequestingMove != player) return;

        event.setCancelled(true);
        move = event.getRawMessage().toPlain().trim();

        /* ------------------------------------------------------------------ */
        /* Tratamentos especiais                                              */
        /* ------------------------------------------------------------------ */
        if (move.equalsIgnoreCase("nevermind")) {
            player.sendMessage(FancyText.getFancyText(
                    ConfigManager.getConfigNode(4,"Messages","Merchant-Told-No").getString()));
            SelfCancellingTask.endTask = true;
            PlayerCommands.playerRequestingMove = null;
            return;
        }

        if (isMoveInBlacklist(move)) {
            player.sendMessage(FancyText.getFancyText(
                    ConfigManager.getConfigNode(4,"Messages","Move-Blacklisted").getString()));
            cancelFlow(); return;
        }

        /* ------------------------------------------------------------------ */
        /* Verifica se o golpe existe via AttackBaseRegistry                  */
        /* ------------------------------------------------------------------ */
        Optional<AttackBase> abOpt = AttackBase.getAttackBase(move);
        if (!abOpt.isPresent()) {
            player.sendMessage(FancyText.getFancyText(
                    ConfigManager.getConfigNode(4,"Messages","Move-Not-Found").getString()));
            cancelFlow(); return;
        }
        // AttackBase ab = abOpt.get();

        /* ------------------------------------------------------------------ */
        /* Economia: verifica dinheiro e espaço de inventário                 */
        /* ------------------------------------------------------------------ */
        EventContext ctx = EventContext.builder()
                .add(EventContextKeys.PLUGIN, TMFactory.getContainer()) .build();

        EconomyService econ = Sponge.getServiceManager()
                                    .provide(EconomyService.class).orElse(null);
        if (econ == null) { cancelFlow(); return; }

        Currency       cur  = econ.getDefaultCurrency();
        UniqueAccount  acc  = econ.getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (acc == null) { cancelFlow(); return; }

        ItemStack dummy = ItemStack.builder().itemType(ItemTypes.DIAMOND_AXE).build();
        if (!player.getInventory().canFit(dummy)) {
            player.sendMessage(FancyText.getFancyText(
                    ConfigManager.getConfigNode(4,"Messages","Not-Enough-Inventory-Space").getString()));
            cancelFlow(); return;
        }

        int price = isMoveInWhitelist(move)
                ? getWhitelistedMovePrice(move)
                : SelfCancellingTask.getDefaultPrice();

        if (acc.getBalance(cur).intValue() < price) {
            player.sendMessage(FancyText.getFancyText(
                    ConfigManager.getConfigNode(4,"Messages","Not-Enough-Money").getString()));
            cancelFlow(); return;
        }

        /* ------------------------------------------------------------------ */
        /* Faz a cobrança e entrega o TM                                      */
        /* ------------------------------------------------------------------ */
        acc.withdraw(cur, BigDecimal.valueOf(price), Cause.of(ctx, TMFactory.getContainer()));
        createAndGiveMove(player, move);

        cancelFlow();
    }

    /* ---------------- Helpers ------------------------------------------- */
    private static void cancelFlow() {
        SelfCancellingTask.endTask = true;
        PlayerCommands.playerRequestingMove = null;
    }

    private static boolean isMoveInWhitelist(String mv) throws ObjectMappingException {
        Map<String,Integer> map = ConfigManager
                .getConfigNode(4,"Moves-Whitelist")
                .getValue(new WhitelistTypeToken());
        return map.containsKey(mv);
    }
    private static int getWhitelistedMovePrice(String mv) throws ObjectMappingException {
        Map<String,Integer> map = ConfigManager
                .getConfigNode(4,"Moves-Whitelist")
                .getValue(new WhitelistTypeToken());
        return map.get(mv);
    }
    private static boolean isMoveInBlacklist(String mv) throws ObjectMappingException {
        List<String> list = ConfigManager
                .getConfigNode(4,"Moves-Blacklist")
                .getList(TypeToken.of(String.class));
        return list.contains(mv);
    }

    private static void createAndGiveMove(Player player, String mv) {
        if (ConfigManager.getConfigNode(0,"TMs", mv).isVirtual()) {
            Sponge.getCommandManager().process(
                    Sponge.getServer().getConsole(),
                    "tmfactory admin createtm 999 " + mv);
        }
        Sponge.getCommandManager().process(
                Sponge.getServer().getConsole(),
                "tmfactory admin givetm " + player.getName() + " 1 " + mv);

        player.sendMessage(FancyText.getFancyText(
                ConfigManager.getConfigNode(4,"Messages","Move-Created")
                             .getString().replace("%move%", mv)));
    }

    static class WhitelistTypeToken extends TypeToken<Map<String,Integer>> {
        private static final long serialVersionUID = 1L;
    }
}
