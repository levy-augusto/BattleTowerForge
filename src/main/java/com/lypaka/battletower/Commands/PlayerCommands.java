package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Guis.ShopGUI;
import com.lypaka.battletower.Utils.FancyText;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class PlayerCommands {

    public static Player playerRequestingMove;

    public static CommandSpec registerDialogueCommands() {
        return CommandSpec.builder()
                .arguments(GenericArguments.string(Text.of("yes|no")))
                .executor((src, ctx) -> {
                    Player pl  = (Player) src;
                    String arg = ctx.<String>requireOne("yes|no").toLowerCase();

                    if (arg.equals("yes")) {
                        playerRequestingMove = pl;
                        pl.sendMessage(FancyText.getFancyText(
                                ConfigManager.getConfigNode(4,"Messages","Merchant-Told-Yes").getString()));
                    } else {
                        pl.sendMessage(FancyText.getFancyText(
                                ConfigManager.getConfigNode(4,"Messages","Merchant-Told-No").getString()));
                    }
                    return CommandResult.success();
                }).build();
    }

    public static CommandSpec registerPlayerCommands() {
        return CommandSpec.builder()
                .permission("tmfactory.command.shop")
                .arguments(GenericArguments.string(Text.of("tm|tr|hm")))
                .executor((src, ctx) -> {
                    Player pl   = (Player) src;
                    String type = ctx.<String>requireOne("tm|tr|hm").toUpperCase();

                    try {
                        if (type.equals("TM") || type.equals("TR") || type.equals("HM"))
                            ShopGUI.openShopPage(pl, type);
                        else pl.sendMessage(Text.of(TextColors.RED, "Tipo inválido!"));
                    } catch (ObjectMappingException ex) { ex.printStackTrace(); }
                    return CommandResult.success();
                }).build();
    }
}
