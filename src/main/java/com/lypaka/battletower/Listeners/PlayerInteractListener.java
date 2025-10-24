package com.lypaka.battletower.Listeners;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.TMFactory;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Utils.SelfCancellingTask;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.scheduler.Task;

import java.util.ArrayList;
import java.util.List;

public class PlayerInteractListener {

    public static Task task;
    public static int messages = 0;
    public static int startingIndex = 0;
    public static List<String> dialogue = new ArrayList<>();

    @Listener
    public void onInteract(InteractEntityEvent.Secondary.MainHand event, @Root Player player) throws ObjectMappingException {
        Entity entity = (Entity) event.getTargetEntity();

        // Verifica se é um NPCTrainer (Reforged)
        if (!(entity instanceof NPCTrainer)) return;

        World world = entity.getEntityWorld();
        BlockPos pos = entity.getPosition();

        List<String> npcList = ConfigManager.getConfigNode(4, "Merchant-Settings", "Merchant-Locations")
                .getList(TypeToken.of(String.class));

        if (npcList == null) npcList = new ArrayList<>();

        String locationKey = world.getWorldInfo().getWorldName() + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();

        if (npcList.contains(locationKey) && hasInteractPermission(player)) {
            event.setCancelled(true);
            runTellerDialogue(player);
        }
    }

    private static boolean hasInteractPermission(Player player) {
        String permission = ConfigManager.getConfigNode(4, "Access", "Restrict-Permission").getString();
        return permission.equalsIgnoreCase("none") || player.hasPermission(permission);
    }

    private static void runTellerDialogue(Player player) throws ObjectMappingException {
        dialogue = ConfigManager.getConfigNode(4, "Messages", "Merchant-Dialogue")
                .getList(TypeToken.of(String.class));
        messages = dialogue.size();
        task = Task.builder()
                .intervalTicks(15L)
                .execute(new SelfCancellingTask(player))
                .submit(TMFactory.instance);
    }
}
