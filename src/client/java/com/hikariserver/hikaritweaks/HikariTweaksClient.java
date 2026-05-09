package com.hikariserver.hikaritweaks;

import com.hikariserver.hikaritweaks.config.ClientConfigManager;
import com.hikariserver.hikaritweaks.config.HotkeyCallbacks;
import com.hikariserver.hikaritweaks.hotkey.HikariTweaksHotkeyProvider;
import com.hikariserver.hikaritweaks.litematica.LitematicaAutoRefreshHandler;
import com.hikariserver.hikaritweaks.restock.AutoRestockHotbarHandler;
import com.hikariserver.hikaritweaks.restock.TotemRestockHandler;
import com.hikariserver.hikaritweaks.scoreboard.ScoreboardHudRenderer;
import com.hikariserver.hikaritweaks.scoreboard.ScoreboardPacketClient;
import com.hikariserver.hikaritweaks.update.UpdateCheckerService;
import com.hikariserver.hikaritweaks.warning.DurabilityWarningHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

public class HikariTweaksClient implements ClientModInitializer {

    public static final String MOD_ID = "hikari-tweaks";
    public static final String MOD_NAME = "Hikari-Tweaks";
    public static final String MOD_VERSION = "1.0.5";
    private static String cachedVersion;

    @Override
    public void onInitializeClient() {
        ClientConfigManager.load();

        ConfigManager.getInstance().registerConfigHandler(MOD_ID, ClientConfigManager.CONFIG_HANDLER);

        HotkeyCallbacks.init();

        InputEventHandler.getKeybindManager().registerKeybindProvider(HikariTweaksHotkeyProvider.INSTANCE);
        InputEventHandler.getKeybindManager().updateUsedKeys();

        ScoreboardPacketClient.register();
        UpdateCheckerService.start();

        ClientTickEvents.END_CLIENT_TICK.register(AutoRestockHotbarHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TotemRestockHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DurabilityWarningHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(LitematicaAutoRefreshHandler::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ScoreboardPacketClient.setOnListUpdated(null);
            ScoreboardPacketClient.setOnRankingUpdated(null);
            ScoreboardPacketClient.resetHiddenState();
            UpdateCheckerService.onDisconnect();
            LitematicaAutoRefreshHandler.reset();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ScoreboardPacketClient.resetHiddenState();
            ScoreboardHudRenderer.resetPage();
            UpdateCheckerService.onJoin(client);
            if (client.player != null) {
                client.player.sendMessage(Text.of("[" + MOD_NAME + "] " + getModVersion() + " Running"), false);
            }
        });
    }

    public static String getModVersion() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        cachedVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        return cachedVersion;
    }
}