package com.lypaka.battletower;

import com.lypaka.battletower.Commands.BattleTowerCommand;
import com.lypaka.battletower.Config.ConfigManager;
import com.lypaka.battletower.Listeners.*;
import com.lypaka.battletower.random.RandomPoolService;
import com.lypaka.battletower.random.RandomTeamApplier;
import com.lypaka.battletower.random.TeamFactoryBridge;
import com.lypaka.battletower.random.TeamRandomizer;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;
import com.lypaka.battletower.limits.ChallengeQuotaService;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Random;

@Mod(
    modid = "battletower",
    name = "BattleTower",
    version = "1.0.0-Reforged",
    acceptableRemoteVersions = "*"
)
public class BattleTower {

    public static final String MOD_ID   = "battletower";
    public static final String MOD_NAME = "BattleTower";
    public static final String VERSION  = "1.0.0-Reforged";

    @Mod.Instance(MOD_ID)
    public static BattleTower INSTANCE;

    public static Logger logger = LogManager.getLogger("Battle Tower");
    public static MinecraftServer server;
    public static boolean isForgeEssentialsLoaded = false;
    public static boolean isSpongeLoaded = false;
    public static boolean isSpigotLoaded = false;
    public static boolean isCratesSystemLoaded = false;
    public static Random random = new Random();
    private static ChallengeQuotaService QUOTA;
    public static ChallengeQuotaService getQuota() { return QUOTA; }

    private static RandomTeamApplier TEAM_APPLIER;
    public static RandomTeamApplier getTeamApplier() { return TEAM_APPLIER; }

    public static final Path MOD_CONFIG_DIR = Paths.get("config", "battletower");
    static {
        try { Files.createDirectories(MOD_CONFIG_DIR); } catch (IOException ignored) {}
    }

    /* ---------------------- PRE-INIT ---------------------- */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger.info("Loading Battle Tower version: {}", VERSION);
        INSTANCE = this;

        try { Files.createDirectories(Paths.get("config", "battletower")); } catch (IOException e) { e.printStackTrace(); }

        // carrega/garante configs
        try {
            ConfigManager.setupAll(Paths.get("config", "tmfactory"), "default");
        } catch (IOException e) {
            logger.error("Failed to load BattleTower configs!", e);
        }

        if (ConfigManager.getConfigNode(0, "Misc", "Permission-Mode").isVirtual()) {
            ConfigManager.getConfigNode(0, "Misc", "Permission-Mode")
                    .setComment("Sets how Battle Tower checks player permissions")
                    .setValue("forgeessentials");
            ConfigManager.getConfigNode(0, "Misc", "Cooldown")
                    .setComment("Sets the cooldown between Battle Tower attempts. Use \"disabled\" to disable. Example: \"5 minutes\".")
                    .setValue("disabled");
        }

        // salva alterações
        ConfigManager.saveAll();

        ConfigManager.ensureRandomDefaults();
        ConfigManager.saveAll();


        // <<< aqui inicializa o serviço random
        initQuotaService();
        initRandomTeamServices();
    }


    public static void initQuotaService() {
        // usa o mesmo arquivo 0 (battle-rooms.conf)
        QUOTA = ChallengeQuotaService.fromConfig();
    }

    public static void initRandomTeamServices() {
        CommentedConfigurationNode randomRoot = ConfigManager.getRandomConfRoot();
        if (randomRoot == null) { 
            TEAM_APPLIER = null;
            logger.info("[BT-Random] random.conf nao encontrado -> serviço desativado");
             return; }

        RandomPoolService pool = new RandomPoolService(randomRoot);
        logger.info("[BT-Random] enabled=" + pool.isEnabled());

        TeamRandomizer randomizer = new TeamRandomizer();
        TeamFactoryBridge bridge = new TeamFactoryBridge();

        TEAM_APPLIER = new RandomTeamApplier(pool, randomizer, bridge);
        logger.info("[BT-Random] serviço inicializado com sucesso");
    }

    /* ---------------------- INIT ---------------------- */
    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Pixelmon EVENT_BUS (eventos do Pixelmon)
        Pixelmon.EVENT_BUS.register(new BattleFlowListener());   // nosso listener que decide próximo tier/entrada
        MinecraftForge.EVENT_BUS.register(new CommandInterceptor());
        Pixelmon.EVENT_BUS.register(new EXPListener());          // ExperienceGainEvent
        Pixelmon.EVENT_BUS.register(new BattleStartListener());  // se usa eventos do Pixelmon
        //Pixelmon.EVENT_BUS.register(new BattleEndListener());    // se usa eventos do Pixelmon

        // Forge EVENT_BUS (eventos vanilla/forge)
        MinecraftForge.EVENT_BUS.register(new JoinListener());
        MinecraftForge.EVENT_BUS.register(new InteractionListeners());
        MinecraftForge.EVENT_BUS.register(new CommandListener());
        MinecraftForge.EVENT_BUS.register(new HeadBossClickListener());
        MinecraftForge.EVENT_BUS.register(new TrainerClickListener());
   
    }

    /* ---------------------- POST-INIT ---------------------- */
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (Loader.isModLoaded("spongeforge")) {
            isSpongeLoaded = true;
            logger.info("Detected Sponge!");
            if (Loader.isModLoaded("huskycrates")) {
                isCratesSystemLoaded = true; logger.info("Detected HuskyCrates!");
            } else if (Loader.isModLoaded("gwm_crates")) {
                isCratesSystemLoaded = true; logger.info("Detected GWM Crates!");
            }
        }
        if (Loader.isModLoaded("forgeessentials")) {
            isForgeEssentialsLoaded = true; logger.info("Detected ForgeEssentials!");
        }
    }

    /* ---------------------- SERVER ---------------------- */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new BattleTowerCommand());
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        server = FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static Path getDir() {
        try { Files.createDirectories(MOD_CONFIG_DIR); } catch (IOException ignored) {}
        return MOD_CONFIG_DIR;
    }
}
