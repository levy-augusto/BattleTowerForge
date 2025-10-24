package com.lypaka.battletower.Commands;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

public class BattleTowerCommand extends CommandTreeBase {

    private final List<ICommand> children = new ArrayList<>();

    public BattleTowerCommand() {
        add(new AddBPCommand());
        add(new CheckCommand());
        add(new KickCommand());
        add(new ReloadCommand());
        add(new DimIDCommand());
        add(new SetRoomCommand());
        add(new SetTrainerCommand());
        add(new SetBossCommand());
        add(new SetReceptionCommand());
        add(new SetShopCommand());
        add(new ReloadRandomCommand()); 
    }

    /** método helper para registrar no tree e na lista */
    private void add(ICommand cmd) {
        this.addSubcommand(cmd);
        this.children.add(cmd);
    }

    @Override
    public String getName() {
        return "battletower";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/battletower <add|check|kick|reload|reloadrandom|dim|setroom|settrainer|setboss|setreception|setshop>";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // root sempre liberado; cada subcomando faz a própria checagem
        return true;
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("bt");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos pos) {
        if (args.length == 1) {
            // só sugere subcomandos que o sender pode usar
            return this.children.stream()
                    .filter(cmd -> cmd.checkPermission(server, sender))
                    .map(ICommand::getName)
                    .filter(n -> n.regionMatches(true, 0, args[0], 0, args[0].length()))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            ICommand child = this.children.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(args[0]) ||
                                 c.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(args[0])))
                    .findFirst()
                    .orElse(null);
            if (child != null && child.checkPermission(server, sender)) {
                String[] childArgs = Arrays.copyOfRange(args, 1, args.length);
                return child.getTabCompletions(server, sender, childArgs, pos);
            }
        }
        return Collections.emptyList();
    }
}
