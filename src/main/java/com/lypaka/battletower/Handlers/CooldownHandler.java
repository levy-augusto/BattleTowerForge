// src/main/java/com/lypaka/battletower/Handlers/CooldownHandler.java
package com.lypaka.battletower.Handlers;

import com.lypaka.battletower.Config.ConfigGetters;
import com.lypaka.battletower.Config.ConfigManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

public class CooldownHandler {

    /** player-data/<uuid>.conf → millis (epoch) de quando o player PODERÁ tentar novamente */
    private static final String PATH_NEXT_ALLOWED = "Cooldown.Next-Allowed-At";

    /** true se agora ainda é antes do próximo permitido */
    public static boolean isOnCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        long next = ConfigManager.getPlayerConfigNode(uuid, PATH_NEXT_ALLOWED).getLong(0L);
        return next > now;
    }

    /** quanto falta (ms) até liberar */
    public static long millisLeft(UUID uuid) {
        long now = System.currentTimeMillis();
        long next = ConfigManager.getPlayerConfigNode(uuid, PATH_NEXT_ALLOWED).getLong(0L);
        return Math.max(0L, next - now);
    }

    /** inicia cooldown a partir de agora, usando Tower.challenge-cooldown (minutos) */
    public static void startCooldownNow(UUID uuid) {
        int cdMin = Math.max(0, ConfigGetters.challengeCooldownMinutes);
        long next = System.currentTimeMillis() + (cdMin * 60_000L);
        ConfigManager.getPlayerConfigNode(uuid, PATH_NEXT_ALLOWED).setValue(next);
        ConfigManager.savePlayer(uuid);
    }

    /** checa + envia mensagem amigável; true = pode continuar, false = bloqueado */
    public static boolean guardOrInform(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        if (!isOnCooldown(id)) return true;

        long leftMs = millisLeft(id);
        String nice = formatMillis(leftMs);
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "Você ainda deve esperar " +
                TextFormatting.YELLOW + nice +
                TextFormatting.RED + " para desafiar novamente a Battle Tower."
        ));
        return false;
    }

    /** formata 01h 23m / 45m / 12s */
    public static String formatMillis(long ms) {
        long s = ms / 1000;
        long h = s / 3600; s %= 3600;
        long m = s / 60;   s %= 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        if (m > 0) return m + "m";
        return s + "s";
    }
}
