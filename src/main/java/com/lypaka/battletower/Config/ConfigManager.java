package com.lypaka.battletower.Config;

import com.lypaka.battletower.TMFactory;
import info.pixelmon.repack.ninja.leaping.configurate.ConfigurationOptions;
import info.pixelmon.repack.ninja.leaping.configurate.commented.CommentedConfigurationNode;
import info.pixelmon.repack.ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import info.pixelmon.repack.ninja.leaping.configurate.loader.ConfigurationLoader;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.asset.Asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.IntStream;
import info.pixelmon.repack.ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;


/**
 * Indices dos arquivos em config/battletower:
 *   0 → battle-rooms.conf
 *   1 → bp-shop.conf
 *   2 → npc-settings.conf
 *   3 → room-settings.conf
 *   4 → random-pokemon.conf
 *   5 → random-pokemon.conf
 */
public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger("BattleTower");

    /* -------------------- ARQUIVOS BASE (battletower/) -------------------- */
    private static final String[] BT_FILE_NAMES = {
            "battle-rooms", "bp-shop", "npc-settings", "room-settings", "random-pokemon", "tower"
    };
    private static final Path BT_DIR = Paths.get("config", "battletower");

    private static final Path[] btPaths = new Path[BT_FILE_NAMES.length];
    private static final CommentedConfigurationNode[] btNodes = new CommentedConfigurationNode[BT_FILE_NAMES.length];
    private static final List<HoconConfigurationLoader> btLoaders = new ArrayList<>(BT_FILE_NAMES.length);

    /* -------------------- PLAYER DATA (battletower/player-data/) -------------------- */
    private static final Map<UUID, Path> btPlayerPaths = new HashMap<>();
    private static final Map<UUID, HoconConfigurationLoader> btPlayerLoaders = new HashMap<>();
    private static final Map<UUID, CommentedConfigurationNode> btPlayerNodes = new HashMap<>();

    /* -------------------- TM FACTORY -------------------- */
    private static final String[] TM_FILE_NAMES = {
            "TMs.conf", "TRs.conf", "HMs.conf", "shop.conf", "merchant.conf", "custom-movepools.conf"
    };
    private static Path tmDir;
    private static final Path[] tmPaths = new Path[TM_FILE_NAMES.length];
    private static final CommentedConfigurationNode[] tmNodes = new CommentedConfigurationNode[TM_FILE_NAMES.length];
    private static final List<ConfigurationLoader<CommentedConfigurationNode>> tmLoaders =
            new ArrayList<>(TM_FILE_NAMES.length);

    /* -------------------- NPC PRESETS (battletower/npcs/<folder>/1.conf..10.conf) -------------------- */
    private static final String[] NPC_FILE_NAMES = {
            "1.conf", "2.conf", "3.conf", "4.conf", "5.conf",
            "6.conf", "7.conf", "8.conf", "9.conf", "10.conf"
    };
    private static Path[][] npcPaths;
    private static CommentedConfigurationNode[][] npcNodes;

    /* ====================================================================== */
    /*  PUBLIC: inicializacao                                                 */
    /* ====================================================================== */

    /**
     * Inicializa todas as configuracões.
     * @param tmFolder   pasta onde ficarao os arquivos da TMFactory
     * @param npcFolders nomes das pastas de NPC (ex.: "default", "gym1", …)
     */
    public static void setupAll(Path tmFolder, String... npcFolders) throws IOException {
        setupBattleTower();
        setupTMFactory(tmFolder);
        loadNPCConfigs(npcFolders);
    }

    /* ====================================================================== */
    /*  BATTLE TOWER (arquivos principais)                                    */
    /* ====================================================================== */

    private static void setupBattleTower() throws IOException {
        Files.createDirectories(BT_DIR);
        Files.createDirectories(BT_DIR.resolve("player-data"));

        for (int i = 0; i < BT_FILE_NAMES.length; i++) {
            Path path = BT_DIR.resolve(BT_FILE_NAMES[i] + ".conf");
            btPaths[i] = path;
            if (!Files.exists(path)) Files.createFile(path);

            HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(path).build();
            btLoaders.add(loader);
            btNodes[i] = loader.load();
        }
    }

    /* ====================================================================== */
    /*  TM FACTORY                                                            */
    /* ====================================================================== */

    private static void setupTMFactory(Path folder) throws IOException {
        tmDir = folder;
        if (!Files.exists(tmDir)) Files.createDirectories(tmDir);

        // evita duplicar loaders em reload
        tmLoaders.clear();
        Arrays.fill(tmPaths, null);
        Arrays.fill(tmNodes, null);

        for (int i = 0; i < TM_FILE_NAMES.length; i++) {
            final int index = i;
            tmPaths[index] = tmDir.resolve(TM_FILE_NAMES[index]);

            Optional<Asset> asset = TMFactory.getContainer().getAsset(TM_FILE_NAMES[index]);
            asset.ifPresent(a -> {
                try {
                    a.copyToFile(tmPaths[index], false, true);
                } catch (IOException e) {
                    TMFactory.getLogger().error("Nao foi possivel copiar o config default " + TM_FILE_NAMES[index], e);
                }
            });

            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(tmPaths[index]).build();
            tmLoaders.add(loader);
            tmNodes[index] = loader.load();
        }
    }

    public static void reloadTMFactory(Path folder) throws IOException {
        setupTMFactory(folder);
    }

    /* ====================================================================== */
    /*  NPC PRESETS                                                           */
    /* ====================================================================== */

    private static void loadNPCConfigs(String[] folders) throws IOException {
        int folderCount = folders.length, fileCount = NPC_FILE_NAMES.length;

        npcPaths = new Path[folderCount][fileCount];
        npcNodes = new CommentedConfigurationNode[folderCount][fileCount];

        for (int i = 0; i < folderCount; i++) {
            Path folderPath = Paths.get("config/battletower/npcs/" + folders[i]);
            if (!Files.exists(folderPath)) Files.createDirectories(folderPath);

            for (int j = 0; j < fileCount; j++) {
                Path file = folderPath.resolve(NPC_FILE_NAMES[j]);
                npcPaths[i][j] = file;

                if (!Files.exists(file)) copyDefaultNPC(file);

                HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(file).build();
                npcNodes[i][j] = loader.load(ConfigurationOptions.defaults());
            }
        }
    }

    private static void copyDefaultNPC(Path dest) {
        try (InputStream in = ConfigManager.class
                .getClassLoader()
                .getResourceAsStream("assets/battletower/npc-template.conf")) {

            if (in == null) {
                LOGGER.warn("npc-template.conf nao encontrado; criando arquivo vazio: " + dest);
                Files.createFile(dest);
                return;
            }
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            LOGGER.error("Falha ao copiar npc-template.conf", e);
        }
    }

    /* ====================================================================== */
    /*  GETTERS / SETTERS de nós                                              */
    /* ====================================================================== */

    public static CommentedConfigurationNode getConfigNode(int index, Object... path) {
        if (index < 0 || index >= btNodes.length) {
            throw new IndexOutOfBoundsException("Config index inválido: " + index);
        }
        return btNodes[index].getNode(path);
    }

    /** Garante que o arquivo do jogador exista e esteja carregado. */
    private static CommentedConfigurationNode ensurePlayerNode(UUID uuid) {
        CommentedConfigurationNode root = btPlayerNodes.get(uuid);
        if (root != null) return root;

        try {
            createPlayerConfig(uuid);
            return btPlayerNodes.get(uuid);
        } catch (IOException e) {
            LOGGER.error("Falha ao criar player config para " + uuid, e);
            // fallback: cria nó vazio em memória para evitar NPE
            try {
                Path file = BT_DIR.resolve("player-data").resolve(uuid.toString() + ".conf");
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(file).build();
                CommentedConfigurationNode node = loader.createEmptyNode(ConfigurationOptions.defaults());
                btPlayerPaths.put(uuid, file);
                btPlayerLoaders.put(uuid, loader);
                btPlayerNodes.put(uuid, node);
                return node;
            } catch (Throwable t) {
                // se até o fallback falhar, retorna um nó "fantasma" que nao salva
            return SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults());

            }
        }
    }

    public static CommentedConfigurationNode getPlayerConfigNode(UUID uuid, Object... path) {
        CommentedConfigurationNode root = ensurePlayerNode(uuid);
        return root.getNode(path);
    }

    public static void createPlayerConfig(UUID uuid) throws IOException {
        Path file = BT_DIR.resolve("player-data").resolve(uuid.toString() + ".conf");
        if (!Files.exists(file)) Files.createFile(file);

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(file).build();
        CommentedConfigurationNode node = loader.load();

        btPlayerPaths.put(uuid, file);
        btPlayerLoaders.put(uuid, loader);
        btPlayerNodes.put(uuid, node);
    }

    public static CommentedConfigurationNode getTMConfigNode(int index, Object... path) {
        return tmNodes[index].getNode(path);
    }

    /**
     * Atencao: indices sao 0-based para 'folder' e 'file'.
     * folder = indice da pasta em npcFolders; file = 0..9 para 1.conf..10.conf
     */
    public static CommentedConfigurationNode getNPCConfigNode(int folder, int file, Object... path) {
        if (folder < 0 || folder >= npcNodes.length) {
            throw new IndexOutOfBoundsException("NPC folder idx inválido: " + folder);
        }
        if (file < 0 || file >= npcNodes[folder].length) {
            throw new IndexOutOfBoundsException("NPC file idx inválido: " + file);
        }
        return npcNodes[folder][file].getNode(path);
    }

    /* ====================================================================== */
    /*  SAVE                                                                  */
    /* ====================================================================== */

    public static void savePlayerConfig(UUID uuid) throws IOException {
        HoconConfigurationLoader loader = btPlayerLoaders.get(uuid);
        CommentedConfigurationNode node = btPlayerNodes.get(uuid);
        if (loader != null && node != null) loader.save(node);
    }

    public static void savePlayer(UUID uuid) {
        try {
            savePlayerConfig(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAll() {
        Runnable job = () -> {
            int nBT = Math.min(btLoaders.size(), btNodes.length);
            IntStream.range(0, nBT).forEach(i -> {
                try { btLoaders.get(i).save(btNodes[i]); } catch (IOException ignored) {}
            });

            int nTM = Math.min(tmLoaders.size(), tmNodes.length);
            IntStream.range(0, nTM).forEach(i -> {
                try { tmLoaders.get(i).save(tmNodes[i]); } catch (IOException ignored) {}
            });
        };

        // Se Sponge estiver carregado, tenta usar o scheduler com o PluginContainer correto;
        // caso contrário (ou se nao achar o plugin), executa sincrono.
        try {
            if (com.lypaka.battletower.BattleTower.isSpongeLoaded) {
                java.util.Optional<org.spongepowered.api.plugin.PluginContainer> owner =
                        org.spongepowered.api.Sponge.getPluginManager().getPlugin("battletower");
                if (owner.isPresent()) {
                    org.spongepowered.api.scheduler.Task.builder()
                            .execute(job)
                            .async()
                            .submit(owner.get());
                    return;
                }
            }
        } catch (Throwable ignored) {
            // qualquer erro aqui cai no fallback sincrono
        }

        // Fallback sincrono (seguro em preInit)
        job.run();
    }

    /* ====================================================================== */
    /*  UTIL                                                                  */
    /* ====================================================================== */

    public static int getWorldID(String worldName) {
        switch (worldName.toLowerCase()) {
            case "overworld":
            case "world":
            case "default":
                return 0;
            case "nether":
            case "the_nether":
                return -1;
            case "end":
            case "the_end":
                return 1;
            default:
                try {
                    return Integer.parseInt(worldName);
                } catch (NumberFormatException e) {
                    return -100; // inválido / nao encontrado
                }
        }
    }

    /** Define/atualiza um valor no arquivo do jogador (auto-cria se faltar). */
    public static void setPlayerValue(EntityPlayer player, String path, Object value) {
        CommentedConfigurationNode node = ensurePlayerNode(player.getUniqueID());
        String[] parts = path.split("\\.");
        CommentedConfigurationNode current = node;
        for (String part : parts) current = current.getNode(part);
        current.setValue(value);
    }

    public static Path getTMDir() {
        return tmDir;
    }


    public static CommentedConfigurationNode getRandomConfRoot() {
        // 4 é o índice do random-pokemon.conf
        return btNodes[4];
    }

    /** Garante chaves básicas no random-pokemon.conf + um exemplo mínimo. */
    public static void ensureRandomDefaults() {
        CommentedConfigurationNode root = btNodes[4];
        if (root == null) return;

        if (root.getNode("enabled").isVirtual()) {
            root.getNode("enabled").setComment("Liga/desliga o time aleatorio via arquivo").setValue(true);
        }
        CommentedConfigurationNode tiers = root.getNode("tiers");
        if (tiers.isVirtual()) {
            CommentedConfigurationNode t1 = tiers.getNode("1");
            t1.getNode("teamSize").setValue(3);
            CommentedConfigurationNode pool = t1.getNode("defaultPool");
pool.setValue(Arrays.asList(
    "pikachu lvl:25 nature:timid moves:thunderbolt,quickattack,iron_tail,volt_tackle",
    "bulbasaur lvl:24 nature:modest moves:razor_leaf,sleep_powder,leech_seed,sludge_bomb",
    "squirtle lvl:24 nature:bold moves:water_pulse,bite,rapid_spin,protect"
));

        }
    }

    public static void reloadRandom() throws IOException {
        if (btLoaders.size() > 4) {
            btNodes[4] = btLoaders.get(4).load();
        }
    }

}
