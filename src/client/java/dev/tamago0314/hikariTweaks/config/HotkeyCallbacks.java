package dev.tamago0314.hikariTweaks.config;

import dev.tamago0314.hikariTweaks.gui.HikariTweaksConfigScreen;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardHudRenderer;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.MinecraftClient;

public final class HotkeyCallbacks {

    private HotkeyCallbacks() {}

    public static void init() {
        // トグル系
        attachToggle(TweaksOptions.FIX_BEACON_RANGE_FREE_CAM);
        attachToggle(TweaksOptions.DURABILITY_WARNING_ENABLED);
        attachToggle(TweaksOptions.AUTO_RESTOCK_HOTBAR);
        attachToggle(TweaksOptions.TOTEM_RESTOCK);

        // Config画面を開くキー
        TweaksOptions.OPEN_CONFIG.getKeybind().setCallback(new IHotkeyCallback() {
            @Override
            public boolean onKeyAction(KeyAction action, IKeybind key) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> mc.setScreen(new HikariTweaksConfigScreen(mc.currentScreen)));
                }
                return true;
            }
        });

        // スコアボード次ページ
        TweaksOptions.SCOREBOARD_NEXT_PAGE.getKeybind().setCallback(new IHotkeyCallback() {
            @Override
            public boolean onKeyAction(KeyAction action, IKeybind key) {
                ScoreboardHudRenderer.nextPage();
                return true;
            }
        });

        // スコアボード前ページ
        TweaksOptions.SCOREBOARD_PREV_PAGE.getKeybind().setCallback(new IHotkeyCallback() {
            @Override
            public boolean onKeyAction(KeyAction action, IKeybind key) {
                ScoreboardHudRenderer.prevPage();
                return true;
            }
        });
    }

    private static void attachToggle(fi.dy.masa.malilib.config.IHotkeyTogglable config) {
        config.getKeybind().setCallback(new IHotkeyCallback() {
            @Override
            public boolean onKeyAction(KeyAction action, IKeybind key) {
                config.toggleBooleanValue();
                boolean enabled = config.getBooleanValue();
                String status = enabled
                        ? GuiBase.TXT_GREEN + "ON" + GuiBase.TXT_RST
                        : GuiBase.TXT_RED  + "OFF" + GuiBase.TXT_RST;
                InfoUtils.printActionbarMessage("%s: %s", config.getPrettyName(), status);
                TweaksOptions.writeToConfig(ClientConfigManager.config);
                ClientConfigManager.save();
                return true;
            }
        });
    }
}