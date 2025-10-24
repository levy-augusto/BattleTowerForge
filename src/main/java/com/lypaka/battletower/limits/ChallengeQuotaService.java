// src/main/java/com/lypaka/battletower/limits/ChallengeQuotaService.java
package com.lypaka.battletower.limits;

import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Handlers.CooldownHandler;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;
import net.minecraft.entity.player.EntityPlayer;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class ChallengeQuotaService {

    public enum Mode { DAILY, DURATION }

    public static class Result {
        public final boolean allowed;
        public final long remainingMillis;
        public Result(boolean allowed, long remainingMillis) {
            this.allowed = allowed;
            this.remainingMillis = remainingMillis;
        }
    }

    public static class Cfg {
        boolean enabled;
        Mode mode;
        /** Valor padrão — será sobrescrito por permissões por jogador. */
        int attemptsDefault;
        boolean perTier;
        LocalTime dailyReset;
        ZoneId zone;
        long durationMillis;
        String blockedMessage;
    }

    private final Cfg cfg;

    public ChallengeQuotaService(Cfg cfg) { this.cfg = cfg; }

    /* =============================== API PÚBLICA =============================== */

    /** Consulta a quota da janela atual SEM consumir tentativa. */
    public Result checkOnly(EntityPlayer player, Integer tierOrNull) {
        if (!cfg.enabled) return new Result(true, 0);

        final UUID id = player.getUniqueID();
        final String scopeKey = cfg.perTier ? ("tier-" + (tierOrNull == null ? 0 : tierOrNull)) : "global";

        final long now = System.currentTimeMillis();
        Window w = currentWindow(now);

        final int attemptsCap = resolveAttemptsCapFor(player);
        long used = ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "used").getLong(0L);
        long storedStart = ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "windowStart").getLong(0L);

        // Se a janela trocou, considere "used" como 0 virtualmente
        if (storedStart != w.startMillis) {
            if (attemptsCap <= 0) return new Result(false, w.endMillis - now);
            return new Result(true, 0);
        }

        if (used >= attemptsCap) {
            long remaining = Math.max(0L, w.endMillis - now);
            return new Result(false, remaining);
        }

        return new Result(true, 0);
    }

    /**
     * Verifica e CONSOME 1 tentativa, se permitido.
     * Se esta foi a última tentativa da janela, liga o cooldown para a próxima tentativa.
     */
    public Result consumeIfAllowed(EntityPlayer player, Integer tierOrNull) {
        if (!cfg.enabled) return new Result(true, 0);
        final UUID id = player.getUniqueID();

        String scopeKey = cfg.perTier ? ("tier-" + (tierOrNull == null ? 0 : tierOrNull)) : "global";

        long now = System.currentTimeMillis();
        Window w = currentWindow(now);

        long used = ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "used").getLong(0L);
        long storedWindowStart = ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "windowStart").getLong(0L);


        System.out.println("[BT-QuotaDebug] Before consume: used=" + used + " windowStart=" + storedWindowStart + " nowWindow=" + w.startMillis);

        if (storedWindowStart != w.startMillis) {
            // reset janela
            used = 0;
            ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "windowStart").setValue(w.startMillis);
            ConfigManager.savePlayer(id);
            System.out.println("[BT-QuotaDebug] Window reset, starting from 0");
        }

        int attemptsCap = resolveAttemptsCapFor(player);
            if (used >= attemptsCap) {
                long remaining = Math.max(0, w.endMillis - now);
                System.out.println("[BT-QuotaDebug] Blocked! attempts=" + used + " limit=" + attemptsCap);
                return new Result(false, remaining);
            }

        // consome
        used++;
        ConfigManager.getPlayerConfigNode(id, "quota", scopeKey, "used").setValue(used);
        ConfigManager.savePlayer(id);


        System.out.println("[BT-QuotaDebug] Consumed -> now used=" + used + "/" + attemptsCap);

        return new Result(true, 0);
    }

    /** Formata ms em "Xd Xh Xm Xs". */
    public String formatRemaining(long ms) {
        long s = Math.max(0, ms) / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;    s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0) sb.append(s).append("s");
        return sb.toString().trim();
    }

    public String blockedMessageWith(String base, long remaining) {
        String msg = (base == null || base.isEmpty())
                ? "&cVocê já fez seu desafio. Volte em {time_left}."
                : base;
        return msg.replace("{time_left}", formatRemaining(remaining));
    }

    /* ========================== CONSTRUÇÃO VIA CONFIG ========================== */

    /** Lê de tower.conf (índice 5) → Tower.Challenge-Quota */
    public static ChallengeQuotaService fromConfig() {
        CommentedConfigurationNode n = ConfigManager.getConfigNode(0, "Challenge-Quota");

        Cfg c = new Cfg();
        c.enabled = n.getNode("Enabled").getBoolean(false);

        String modeStr = n.getNode("Mode").getString("daily").toLowerCase(Locale.ROOT);
        c.mode = "duration".equals(modeStr) ? Mode.DURATION : Mode.DAILY;

        c.attemptsDefault = Math.max(1, n.getNode("Attempts-Default").getInt(1));
        c.perTier = n.getNode("Per-Tier").getBoolean(false);

        String tz = n.getNode("Timezone").getString("America/Sao_Paulo");
        try { c.zone = ZoneId.of(tz); } catch (Exception e) { c.zone = ZoneId.of("America/Sao_Paulo"); }

        String daily = n.getNode("Daily-Reset").getString("03:00");
        c.dailyReset = parseLocalTime(daily);

        String dur = n.getNode("Duration").getString("24h");
        c.durationMillis = parseDurationMillis(dur);

        c.blockedMessage = n.getNode("Message-When-Limited")
                .getString("&cVocê já fez seu desafio. Volte em {time_left}.");

        return new ChallengeQuotaService(c);
    }

    /* =============================== INTERNOS =============================== */

    private static class Window { final long startMillis, endMillis; Window(long s,long e){startMillis=s; endMillis=e;} }

    private Window currentWindow(long nowMillis) {
        if (cfg.mode == Mode.DAILY) {
            ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(cfg.zone);
            LocalDate today = now.toLocalDate();
            ZonedDateTime todayReset = ZonedDateTime.of(today, cfg.dailyReset, cfg.zone);
            ZonedDateTime start = now.isBefore(todayReset) ? todayReset.minusDays(1) : todayReset;
            ZonedDateTime end = start.plusDays(1);
            return new Window(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
        } else {
            long dur = Math.max(1000L, cfg.durationMillis);
            long start = (nowMillis / dur) * dur; // tumbling
            long end = start + dur;
            return new Window(start, end);
        }
    }

    private static LocalTime parseLocalTime(String s) {
        try { return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm")); }
        catch (Exception ignore) { return LocalTime.of(3,0); }
    }

    /** Suporta "1d", "24h", "90m", "45s" (case-insensitive). */
    private static long parseDurationMillis(String s) {
        if (s == null) return 86_400_000L;
        s = s.trim().toLowerCase(Locale.ROOT);
        try {
            long totalMs = 0;
            int i = 0;
            while (i < s.length()) {
                int j = i;
                while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                if (j == i) break;
                long num = Long.parseLong(s.substring(i, j));
                int k = j;
                while (k < s.length() && Character.isLetter(s.charAt(k))) k++;
                String unit = s.substring(j, k);
                long mul;
                switch (unit) {
                    case "d": mul = 86_400_000L; break;
                    case "h": mul = 3_600_000L;  break;
                    case "m": mul = 60_000L;     break;
                    case "s": mul = 1_000L;      break;
                    default:  mul = 1_000L;      break;
                }
                totalMs += num * mul;
                i = k;
            }
            return totalMs > 0 ? totalMs : 86_400_000L;
        } catch (Exception e) {
            return 86_400_000L;
        }
    }

    /** Resolve o CAP de tentativas por janela para ESTE jogador (VIP via perm/meta). */
    private int resolveAttemptsCapFor(EntityPlayer player) {
        return Math.max(1, QuotaPermissionResolver.resolveAttemptsFor(player, cfg.attemptsDefault));
    }
}
