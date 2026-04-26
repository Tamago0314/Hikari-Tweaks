package dev.tamago0314.hikariTweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.dy.masa.malilib.config.IConfigHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ClientConfig の読み書きを管理するクラス。
 */
public final class ClientConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("hikari-tweaks.json");

    public static ClientConfig config = new ClientConfig();
    public static final IConfigHandler CONFIG_HANDLER = new IConfigHandler() {
        @Override
        public void load() {
            ClientConfigManager.load();
        }

        @Override
        public void save() {
            ClientConfigManager.save();
        }
    };

    private ClientConfigManager() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            config = new ClientConfig();
            TweaksOptions.loadFromConfig(config);
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
            if (loaded != null) {
                config = loaded;
            }
        } catch (IOException e) {
            config = new ClientConfig();
        }
        config.applyDefaults();
        config.normalize();
        TweaksOptions.loadFromConfig(config);
    }

    public static void save() {
        TweaksOptions.writeToConfig(config);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            // 保存失敗は無視
        }
    }
}
