package com.lypaka.battletower.Commands;

import com.lypaka.battletower.Config.ConfigManager;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationNode;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import com.google.common.reflect.TypeToken;
import java.util.Collections;


public class SetShopCommand extends CommandBase {

    private static final Pattern SHOP_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    @Override
    public String getName() {
        return "setshop";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        // Mostra como você quer que seja usado via root (/bt ou /battletower)
        return "/bt setshop <shopId>";
    }

    @Override
    public void execute(MinecraftServer server,
                        ICommandSender sender,
                        String[] args) throws CommandException {

        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(new TextComponentString("§cUso correto: §e" + getUsage(sender)));
            return;
        }

        String shopId = args[0].trim();
        if (!SHOP_ID_PATTERN.matcher(shopId).matches()) {
            sender.sendMessage(new TextComponentString("§cshopId inválido. Use apenas letras, números, ponto, underline e hífen (máx 32)."));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        BlockPos pos = player.getPosition();
        String worldName = player.world.getWorldInfo().getWorldName();

        // world,x,y,z,shopId
        String locString = worldName + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + shopId;

        ConfigurationNode listNode = ConfigManager.getConfigNode(
                2, "NPC-Settings", "Shopkeeper-Locations");

        List<String> list;
        try {
            // devolve [] se o nó não existir ou não for lista
            list = new ArrayList<>(listNode.getList(TypeToken.of(String.class), Collections.emptyList()));
        } catch (ObjectMappingException e) {
            e.printStackTrace();
            list = new ArrayList<>();
        }

        if (!list.contains(locString)) {
            // (opcional) remove entradas antigas com o mesmo shopId para evitar duplicatas
            // list.removeIf(s -> s.endsWith("," + shopId));
            list.add(locString);
        }

        listNode.setValue(list);
        ConfigManager.saveAll();

        player.sendMessage(new TextComponentString(
                "§aLojista registrado! §7(shopId=§e" + shopId + "§7) em §e" +
                        worldName + " §7@ §e" + pos.getX() + "/" + pos.getY() + "/" + pos.getZ()
        ));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // não usa OP-level vanilla
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return com.lypaka.battletower.Utils.Perms.has(sender,
            "battletower.admin.setshop",
            "battletower.admin.*"
        );
    }
}
