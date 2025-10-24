package com.lypaka.battletower.Utils;

import com.google.common.reflect.TypeToken;
import com.lypaka.battletower.Config.ConfigManager;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import info.pixelmon.repack.ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.*;

/**
 * Gerencia o uso de TMs/TRs/HMs customizados.
 */
public class ItemHandler {

    /* --------- estado usado após o fechamento da GUI -------- */
    private static Pokemon ultimoPokemon;
    private static Player  ultimoJogador;
    private static String  ultimoGolpe;

    /* ---------------- clique com o disco -------------------- */
    @Listener
    public void onDiscUse(InteractEntityEvent.Secondary.MainHand ev,
                          @Root Player spongePlayer) {

        Optional<ItemStack> opt = spongePlayer.getItemInHand(HandTypes.MAIN_HAND);
        if (!opt.isPresent()) return;

        ItemStack held = opt.get();
        String name   = held.get(Keys.DISPLAY_NAME).map(Text::toPlain).orElse("");
        if (name.isEmpty() || name.contains("editor")) return;

        boolean isTM = isCustomDisc(held, "TMs", 0);
        boolean isTR = isCustomDisc(held, "TRs", 1);
        boolean isHM = name.contains("HM");
        if (!isTM && !isTR && !isHM) return;

        ev.setCancelled(true);

        String moveName = name.split(": ")[1].replace("}]", "");
        Attack attack  = new Attack(moveName);

        PartyStorage party = Pixelmon.storageManager
                                 .getParty((EntityPlayerMP) spongePlayer);

        for (Pokemon poke : party.getTeam()) {
            if (poke == null) continue;
            if (!Objects.equals(poke.getOwnerPlayerUUID(), spongePlayer.getUniqueId())) continue;

            if (podeAprender(poke, moveName) || listaExtraPermite(poke, moveName)) {
                aplicarGolpe(poke, attack, moveName, spongePlayer, held);
            } else {
                spongePlayer.sendMessage(Text.of(TextColors.RED,
                        poke.getLocalizedName(), " refuses to learn this move!"));
            }
            break; // trabalha só com o primeiro pokémon válido
        }
    }

    /* --------------- GUI de substituição fecha -------------- */
    @SuppressWarnings("unlikely-arg-type")
    @SubscribeEvent
    public void onGuiClose(PlayerContainerEvent.Close ev) {
        if (!(ev.getEntityPlayer() instanceof EntityPlayerMP)) return;
        Player spongePlayer = (Player) ev.getEntityPlayer();

        if (ultimoJogador == null || !ultimoJogador.getUniqueId().equals(spongePlayer.getUniqueId()))
            return;

        PartyStorage party = Pixelmon.storageManager
                                 .getParty((EntityPlayerMP) spongePlayer);

        for (Pokemon poke : party.getTeam()) {
            if (poke != null && poke == ultimoPokemon &&
                    poke.getMoveset().contains(ultimoGolpe)) {

                spongePlayer.getItemInHand(HandTypes.MAIN_HAND)
                        .ifPresent(it -> it.setQuantity(it.getQuantity() - 1));
                break;
            }
        }
    }

    /* =================== helpers ============================ */

    /** API 8.4.3 – usa AttackBase.isUsableByPokemon */
    private boolean podeAprender(Pokemon poke, String move) {
        return poke.getBaseStats().canLearn(move);
    }

    private boolean isCustomDisc(ItemStack stack, String node, int cfgIdx) {
        return stack.get(Keys.DISPLAY_NAME)
                    .map(Text::toPlain)
                    .map(n -> {
                        String id = n.split(": ")[1].replace("}]", "");
                        return ConfigManager.getConfigNode(cfgIdx, node)
                                            .getString()
                                            .contains(id);
                    }).orElse(false);
    }

    private List<String> pegarMovesExtras(Pokemon poke) {
        try {
            TypeToken<Map<String, List<String>>> token =
                    new TypeToken<Map<String, List<String>>>() {};

            Map<String, List<String>> map = ConfigManager
                    .getConfigNode(5, "Pokemon")
                    .getValue(token);

            if (map == null) return null;

            // Usa o nome do enum, ex.: BULBASAUR, CHARIZARD…
            String speciesKey = poke.getSpecies().name();          
            // ou: String speciesKey = poke.getSpecies().getPokemonName();

            return map.getOrDefault(speciesKey, null);

        } catch (ObjectMappingException e) {
            return null;
        }
    }


    private boolean listaExtraPermite(Pokemon poke, String move) {
        List<String> extra = pegarMovesExtras(poke);
        return extra != null && extra.stream()
                                     .anyMatch(m -> m.equalsIgnoreCase(move));
    }

    private void aplicarGolpe(Pokemon poke, Attack atk, String moveName,
                              Player player, ItemStack held) {

        if (poke.getMoveset().size() < 4) poke.getMoveset().add(atk);
        else                              poke.getMoveset().set(0, atk);

        //poke.updateMoveset();
        held.setQuantity(held.getQuantity() - 1);

        player.sendMessage(Text.of(TextColors.GREEN,
                poke.getLocalizedName(), " learned ", moveName, "!"));

        ultimoPokemon = poke;
        ultimoJogador = player;
        ultimoGolpe   = moveName;
    }
}
