package com.lypaka.battletower;

import com.google.inject.Inject;
import com.lypaka.battletower.Commands.AdminCommands;
import com.lypaka.battletower.Commands.PlayerCommands;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Listeners.ChatListener;
import com.lypaka.battletower.Listeners.PlayerInteractListener;
import com.lypaka.battletower.Utils.ItemHandler;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.nio.file.Path;

@Plugin(
        id          = "tmfactory",
        name        = "TM-Factory",
        version     = "1.4.0-Reforged",
        description = "Plugin de fabricação e entrega de TMs/HMs/TRs para Pixelmon"
)
public class TMFactory {

    @Inject @ConfigDir(sharedRoot = false) private Path configDir;
    @Inject private Logger         logger;
    @Inject private PluginContainer container;
    public  static TMFactory       instance;

   
    @Listener
    public void onPreInit(GamePreInitializationEvent e) {
    instance = this;
    logger.info("TM-Factory carregando…");

    try {
        ConfigManager.reloadTMFactory(configDir);   
    } catch (Exception ex) {
        logger.error("Falha ao ler configs!", ex);
    }

    registerCommands();

        Sponge.getEventManager().registerListeners(this, new ItemHandler());
        Sponge.getEventManager().registerListeners(this, new PlayerInteractListener());
        Sponge.getEventManager().registerListeners(this, new ChatListener());

        MinecraftForge.EVENT_BUS.register(new ItemHandler());
    }

    @Listener
    public void onReload(GameReloadEvent e) {
        try {
            ConfigManager.reloadTMFactory(configDir);
            logger.info("TM-Factory configs recarregadas.");
        } catch (Exception ex) {
            logger.error("Falha ao recarregar configs!", ex);
        }
    }
   

    private void registerCommands() {
        CommandSpec main = CommandSpec.builder()
                .child(PlayerCommands.registerPlayerCommands(),   "shop")
                .child(PlayerCommands.registerDialogueCommands(), "request")
                .child(AdminCommands.registerAdminCommands(),     "admin")
                .executor((src, args) -> CommandResult.success())
                .build();

        Sponge.getCommandManager().register(this, main, "tmfactory");
    }

    public static PluginContainer getContainer() { return instance.container; }
    public static Logger          getLogger()    { return instance.logger;   }
}
