// com.lypaka.battletower.State.SurrenderManager.java
package com.lypaka.battletower.State;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SurrenderManager {
    private static final Set<UUID> SURRENDER_SET = ConcurrentHashMap.newKeySet();

    public static void mark(UUID uuid) { SURRENDER_SET.add(uuid); }
    public static boolean has(UUID uuid) { return SURRENDER_SET.contains(uuid); }
    public static boolean consume(UUID uuid) { return SURRENDER_SET.remove(uuid); }
}
