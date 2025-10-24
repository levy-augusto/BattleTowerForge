package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.ItemCreator;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Locale;

public class AdminCommands {

   
   
   
    private static boolean attackExists(String name) {
        
        return AttackBase.getAttackBase(name).isPresent(); 
    }

    private static String normalize(String s) {
        return s.replace("[", "").replace("]", "").trim();
    }

   
    public static CommandSpec registerAdminCommands() {

       
        CommandSpec create = CommandSpec.builder()
            .arguments(
                    GenericArguments.string(Text.of("tm|tr|hm")),
                    GenericArguments.integer(Text.of("number")),
                    GenericArguments.remainingJoinedStrings(Text.of("move")))
            .permission("tmfactory.command.admin.create")
            .executor((src, ctx) -> {

                String disc = ctx.<String>requireOne("tm|tr|hm").toLowerCase(Locale.ROOT);
                int    num  = ctx.<Integer>requireOne("number");
                String move = normalize(ctx.<String>requireOne("move"));

                if (!attackExists(move)) {
                    src.sendMessage(Text.of(TextColors.RED, "Move inválido!"));
                    return CommandResult.success();
                }

                int idx  = disc.equals("tm") ? 0 : disc.equals("tr") ? 1 : 2;
                String node = disc.toUpperCase(Locale.ROOT) + "s";

                if (!ConfigManager.getConfigNode(idx, node, move).isVirtual()) {
                    src.sendMessage(Text.of(TextColors.RED, "Já existe na config."));
                    return CommandResult.success();
                }

                ConfigManager.getConfigNode(idx, node, move, "Number").setValue(num);
                ConfigManager.saveAll();                                
                src.sendMessage(Text.of(TextColors.GREEN, "Criado com sucesso!"));
                return CommandResult.success();
            }).build();

       
        CommandSpec give = CommandSpec.builder()
            .arguments(
                    GenericArguments.string(Text.of("tm|tr|hm")),
                    GenericArguments.player(Text.of("player")),
                    GenericArguments.integer(Text.of("amount")),
                    GenericArguments.remainingJoinedStrings(Text.of("move")))
            .permission("tmfactory.command.admin.give")
            .executor((src, ctx) -> {

                String disc   = ctx.<String>requireOne("tm|tr|hm").toUpperCase(Locale.ROOT);
                Player player = ctx.requireOne("player");
                int amount    = ctx.requireOne("amount");
                String move   = normalize(ctx.requireOne("move"));

                switch (disc) {
                    case "TM": ItemCreator.giveTM(player, move, amount); break;
                    case "TR": ItemCreator.giveTR(player, move, amount); break;
                    case "HM": ItemCreator.giveHM(player, move, amount); break;
                }
                src.sendMessage(Text.of(TextColors.GREEN, "Item entregue."));
                return CommandResult.success();
            }).build();

       
        CommandSpec reload = CommandSpec.builder()
            .permission("tmfactory.command.admin.reload")
            .executor((src, ctx) -> {
                try {
                    
                    
                    ConfigManager.setupAll(ConfigManager.getTMDir());   
                    src.sendMessage(Text.of(TextColors.GREEN, "Configs recarregadas."));
                } catch (Exception e) {
                    src.sendMessage(Text.of(TextColors.RED, "Falha ao recarregar configs."));
                    e.printStackTrace();
                }
                return CommandResult.success();
            }).build();

       return CommandSpec.builder()
                .child(create, "create")
                .child(give,   "give")
                .child(reload, "reload")
                .executor((s,c) -> CommandResult.success())
                .build();
    }
}