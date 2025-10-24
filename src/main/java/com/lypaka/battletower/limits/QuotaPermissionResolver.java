// src/main/java/com/lypaka/battletower/limits/QuotaPermissionResolver.java
package com.lypaka.battletower.limits;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigManager;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

public class QuotaPermissionResolver {

    /**
     * Resolve tentativas por janela:
     *  1) option/meta (ex.: bt-quota=3)
     *  2) perm nodes (ex.: battletower.quota.3)
     *  3) fallback attemptsDefault
     */
    public static int resolveAttemptsFor(EntityPlayer player, int attemptsDefault) {
        try {
            CommentedConfigurationNode n = ConfigManager.getConfigNode(5, "Tower", "Challenge-Quota", "From-Permissions");
            boolean useMetaFirst = n.getNode("Use-Meta-First").getBoolean(true);
            String metaKey = n.getNode("Meta-Key").getString("bt-quota");
            String permPrefix = n.getNode("Permission-Prefix").getString("battletower.quota.");

            Integer fromMeta = readOptionAsInt(player, metaKey);
            Integer fromNode = readBestQuotaNode(player, permPrefix);

            // ===== DEBUGS IMPORTANTES =====
            System.out.println("[BT-QuotaDebug] resolveAttemptsFor -> default=" + attemptsDefault);
            System.out.println("[BT-QuotaDebug] fromMeta=" + fromMeta + ", fromNode=" + fromNode + ", useMetaFirst=" + useMetaFirst);

            if (useMetaFirst) {
                if (isPositive(fromMeta)) return fromMeta;
                if (isPositive(fromNode)) return fromNode;
            } else {
                if (isPositive(fromNode)) return fromNode;
                if (isPositive(fromMeta)) return fromMeta;
            }
        } catch (Throwable ignored) {}

        return Math.max(1, attemptsDefault);
    }

    /* ========================= Internals ========================= */

    private static boolean isPositive(Integer v) { return v != null && v > 0; }

    /** Lê option/meta como int (ex.: bt-quota=3) usando apenas a Permission API do Sponge. */
    private static Integer readOptionAsInt(EntityPlayer player, String key) {
        if (!BattleTower.isSpongeLoaded) return null;

        PermissionService ps = Sponge.getServiceManager().provide(PermissionService.class).orElse(null);
        if (ps == null) return null;

        Subject subj = getUserSubjectCompat(ps, player);
        if (subj == null) return null;

        Set<Context> ctx = subj.getActiveContexts();
        Optional<String> val = subj.getOption(ctx, key);

        if (!val.isPresent()) return null;

        try {
            int parsed = Integer.parseInt(val.get().trim());
            System.out.println("[BT-QuotaDebug] readOptionAsInt(" + key + ") -> " + parsed);
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Procura o MAIOR node permitido: battletower.quota.N (varre 64→1). */
    private static Integer readBestQuotaNode(EntityPlayer player, String prefix) {
        if (!BattleTower.isSpongeLoaded) return null;

        PermissionService ps = Sponge.getServiceManager().provide(PermissionService.class).orElse(null);
        if (ps == null) return null;

        Subject subj = getUserSubjectCompat(ps, player);
        if (subj == null) return null;

        Set<Context> ctx = subj.getActiveContexts();

        for (int n = 64; n >= 1; n--) {
            String node = prefix + n;
            Tristate ts = subj.getPermissionValue(ctx, node);
            if (ts == Tristate.TRUE) {
                System.out.println("[BT-QuotaDebug] readBestQuotaNode hit -> " + node);
                return n;
            }
        }
        return null;
    }

    /**
     * Compat helper para pegar o Subject do usuário em diferentes versões do Sponge:
     * - Tenta SubjectCollection#getSubject(String) retornando Optional<Subject> ou Subject
     * - Se não existir, tenta SubjectCollection#get(String) idem
     */
    @SuppressWarnings("unchecked")
    private static Subject getUserSubjectCompat(PermissionService ps, EntityPlayer player) {
        try {
            SubjectCollection users = ps.getUserSubjects();
            String key = player.getUniqueID().toString();

            // 1) Tenta getSubject(String)
            try {
                Method m = users.getClass().getMethod("getSubject", String.class);
                Object ret = m.invoke(users, key);
                if (ret instanceof Optional) return ((Optional<Subject>) ret).orElse(null);
                if (ret instanceof Subject)  return (Subject) ret;
            } catch (NoSuchMethodException ignore) {
                // cai para get(String)
            }

            // 2) Tenta get(String)
            try {
                Method m2 = users.getClass().getMethod("get", String.class);
                Object ret2 = m2.invoke(users, key);
                if (ret2 instanceof Optional) return ((Optional<Subject>) ret2).orElse(null);
                if (ret2 instanceof Subject)  return (Subject) ret2;
            } catch (NoSuchMethodException ignore) {
                // nada
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
