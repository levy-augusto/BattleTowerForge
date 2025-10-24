package com.lypaka.battletower.Handlers;

import com.lypaka.battletower.BattleTower;
import com.lypaka.battletower.Config.ConfigManager;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.entity.living.player.Player;

/**
 * Gerencia o sistema de permissões baseado no modo configurado:
 *  - "forgeessentials": checa nível de OP (>=2)
 *  - "luckperms":        usa API da Sponge
 */
public class PermissionHandler {

    /**
     * Verifica se o jogador tem a permissão solicitada.
     *
     * @param player      jogador Forge
     * @param permission  string no formato "mod.permissao"
     */
    public static boolean hasPermission(EntityPlayer player, String permission) {

        String mode = ConfigManager
                .getConfigNode(0, "Misc", "Permission-Mode")
                .getString("forgeessentials")
                .toLowerCase();

        switch (mode) {

            /* ---------------- ForgeEssentials ---------------- */
            case "forgeessentials":
                // OP level 2 ou maior já dá acesso
                return player.canUseCommand(2, "");

            /* ---------------- LuckPerms (Sponge) ------------- */
            case "luckperms":
                if (BattleTower.isSpongeLoaded && player instanceof Player) {
                    return ((Player) player).hasPermission(permission);
                }
                BattleTower.logger.error(
                        "[BattleTower] LuckPerms selecionado, mas Sponge não está presente!");
                return false;

            /* ---------------- Desconhecido ------------------- */
            default:
                BattleTower.logger.error(
                        "[BattleTower] Modo de permissão desconhecido: " + mode);
                return false;
        }
    }
}
