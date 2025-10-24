package com.lypaka.battletower.Listeners;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Guis.PromptGui;
import com.lypaka.battletower.Guis.ShopkeeperLikeUI;
import com.lypaka.battletower.Guis.TeamSelectionMenu;
import com.lypaka.battletower.Utils.DialogueTask;
import com.lypaka.battletower.Utils.FancyText;
import com.pixelmonmod.pixelmon.entities.npcs.NPCChatting;
import com.pixelmonmod.pixelmon.entities.npcs.NPCShopkeeper;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.lypaka.battletower.limits.ChallengeQuotaService;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class InteractionListeners {

    private static final int TOL = 1; // tolerância de blocos no match

    @SubscribeEvent
    public void onNPCInteract(EntityInteract event) throws ObjectMappingException {
        EntityPlayer player = event.getEntityPlayer();

        // DEBUG: evento disparou?
        System.out.println("[BT-ShopDebug] onNPCInteract fired. Target=" + event.getTarget());

        if (event.getTarget() instanceof NPCChatting) {
            NPCChatting npc = (NPCChatting) event.getTarget();
            BlockPos pos = npc.getPosition();
            String worldName = getWorldName(player);

            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            String location = worldName + "," + x + "," + y + "," + z;

            List<String> receptionistLocations = ConfigManager
                    .getConfigNode(2, "NPC-Settings", "Receptionist-Locations")
                    .getList(TypeToken.of(String.class));

            if (receptionistLocations != null && receptionistLocations.contains(location)) {
                event.setCanceled(true);
                boolean isChallenging = ConfigManager.getPlayerConfigNode(player.getUniqueID(),
                        "Current-Challenge-Info", "Is-Challenging").getBoolean(false);

                if (isChallenging) {
                    // Já está numa challenge → mostra prompt padrão (sair/continuar etc)
                    PromptGui.showPromptMenu(player, false);
                } else {
                    // Entrada na recepção → checar QUOTA (somente consulta; sem consumir)
                    handleReceptionQuota(player);
                }
            }

        } else if (event.getTarget() instanceof NPCShopkeeper) {
            NPCShopkeeper npc = (NPCShopkeeper) event.getTarget();
            BlockPos pos = npc.getPosition();
            String worldName = getWorldName(player);

            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            String location = worldName + "," + x + "," + y + "," + z;

            // DEBUG: posição atual do NPC
            System.out.println("[BT-ShopDebug] Click on NPCShopkeeper at " + location);

            // Lê: NPC-Settings -> Shopkeeper-Locations = ["world,x,y,z,shopId", ...]
            List<String> shopkeepers;
            try {
                shopkeepers = ConfigManager
                        .getConfigNode(2, "NPC-Settings", "Shopkeeper-Locations")
                        .getList(TypeToken.of(String.class), new ArrayList<>());
            } catch (ObjectMappingException e) {
                e.printStackTrace();
                shopkeepers = new ArrayList<>();
            }

            System.out.println("[BT-ShopDebug] Entries in Shopkeeper-Locations: " + shopkeepers.size());
            for (String s : shopkeepers) System.out.println("  - " + s);

            String matchedShopId = null;

            // tenta match exato
            for (String entry : shopkeepers) {
                if (entry.startsWith(location + ",")) { // exige vírgula após Z
                    String[] split = entry.split(",");
                    if (split.length >= 5) matchedShopId = split[4].trim();
                    System.out.println("[BT-ShopDebug] Exact match found. shopId=" + matchedShopId);
                    break;
                }
            }

            // se não achou exato, tenta com tolerância de +-1 bloco
            if (matchedShopId == null) {
                for (String entry : shopkeepers) {
                    String[] split = entry.split(",");
                    if (split.length < 5) continue;

                    String w = split[0];
                    Integer ex, ey, ez;
                    try {
                        ex = Integer.parseInt(split[1]);
                        ey = Integer.parseInt(split[2]);
                        ez = Integer.parseInt(split[3]);
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    int dx = Math.abs(ex - x);
                    int dy = Math.abs(ey - y);
                    int dz = Math.abs(ez - z);

                    if (w.equalsIgnoreCase(worldName) && dx <= TOL && dy <= TOL && dz <= TOL) {
                        matchedShopId = split[4].trim();
                        System.out.println("[BT-ShopDebug] Fuzzy match found. shopId=" + matchedShopId +
                                " (dx=" + dx + ",dy=" + dy + ",dz=" + dz + ")");
                        break;
                    }
                }
            }

            if (matchedShopId != null && !matchedShopId.isEmpty()) {
                event.setCanceled(true);
                try {
                    System.out.println("[BT-ShopDebug] Opening shop GUI for shopId=" + matchedShopId);
                    ShopkeeperLikeUI.open(player, matchedShopId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (BattleTower.isSpongeLoaded) {
                        ((org.spongepowered.api.entity.living.player.Player) (Object) player)
                                .sendMessage(FancyText.getFancyText("&cErro ao abrir a loja " + matchedShopId));
                    }
                }
            } else {
                System.out.println("[BT-ShopDebug] No matching shop entry for location " + location);
            }
        }
    }

    private String getWorldName(EntityPlayer player) {
        if (BattleTower.isSpongeLoaded) {
            org.spongepowered.api.entity.living.player.Player sp =
                    (org.spongepowered.api.entity.living.player.Player) (Object) player;
            org.spongepowered.api.world.World w = sp.getWorld();
            return w.getName();
        } else {
            return player.world.getWorldInfo().getWorldName();
        }
    }

    /** Recepção: checa apenas a QUOTA (checkOnly). Se esgotada, barra e mostra tempo restante. */
    private void handleReceptionQuota(EntityPlayer player) throws ObjectMappingException {

        try {
            ChallengeQuotaService quota = BattleTower.getQuota();
            if (quota != null) {
                // Ainda não temos tier escolhido aqui, então passamos null.
                // Se Per-Tier=true, o consumo real usará o tier definitivo na etapa de início da challenge.
                ChallengeQuotaService.Result chk = quota.checkOnly(player, null);
                if (!chk.allowed) {
                    String base = ConfigManager
                            .getConfigNode(5, "Tower", "Challenge-Quota", "Message-When-Limited")
                            .getString("&cVocê já fez seu desafio. Volte em {time_left}.");
                    String msg = quota.blockedMessageWith(base, chk.remainingMillis);

                    player.sendMessage(new TextComponentString(msg.replace("&", "§")));
                    return; // BLOQUEIA AQUI — não abre menus nem prossegue
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(); // log defensivo
        }

        // Se passou na quota, não checamos cooldown aqui (regra VIP: pode usar tentativas restantes).
        // Segue fluxo normal (menus/diálogos)
        TeamSelectionMenu.removePage(player);
        TeamSelectionMenu.clearSelection(player);

        List<String> messages = ConfigManager.getConfigNode(3,
                        "NPC-Messages", "Introduction")
                .getList(TypeToken.of(String.class));
        DialogueTask.timer = new Timer();
        DialogueTask.timer.schedule(new DialogueTask(player, 0, messages, "Introduction"), 0L, 1000L);
    }
}
