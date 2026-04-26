package dev.tamago0314.hikariTweaks;

import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import dev.tamago0314.hikariTweaks.config.HotkeyCallbacks;
import dev.tamago0314.hikariTweaks.hotkey.HikariTweaksHotkeyProvider;
import dev.tamago0314.hikariTweaks.restock.AutoRestockHotbarHandler;
import dev.tamago0314.hikariTweaks.restock.TotemRestockHandler;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardHudRenderer;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardPacketClient;
import dev.tamago0314.hikariTweaks.warning.DurabilityWarningHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

public class HikariTweaksClient implements ClientModInitializer {

    public static final String MOD_ID = "hikari-tweaks";
    public static final String MOD_NAME = "Hikari-Tweaks";
    public static final String MOD_VERSION = "1.0.0";

    @Override
    public void onInitializeClient() {
        ClientConfigManager.load();

        ConfigManager.getInstance().registerConfigHandler(MOD_ID, ClientConfigManager.CONFIG_HANDLER);

        HotkeyCallbacks.init();

        InputEventHandler.getKeybindManager().registerKeybindProvider(HikariTweaksHotkeyProvider.INSTANCE);
        InputEventHandler.getKeybindManager().updateUsedKeys();

        ScoreboardPacketClient.register();

        ClientTickEvents.END_CLIENT_TICK.register(AutoRestockHotbarHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TotemRestockHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DurabilityWarningHandler::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ScoreboardPacketClient.setOnListUpdated(null);
            ScoreboardPacketClient.setOnRankingUpdated(null);
            ScoreboardPacketClient.clearRanking();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ScoreboardPacketClient.clearRanking();
            ScoreboardHudRenderer.resetPage();
            if (client.player != null) {
                client.player.sendMessage(Text.of("[" + MOD_NAME + "] " + MOD_VERSION + " Running"), false);
            }
        });
    }
}
