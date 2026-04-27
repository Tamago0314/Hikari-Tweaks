package dev.tamago0314.hikariTweaks.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.hotkeys.IHotkey;

import java.util.List;

public final class TweaksOptions {

    private static boolean loadingFromConfig = false;

    // ── 補助機能 ──────────────────────────────────────────
    public static final ConfigBooleanHotkeyed FIX_BEACON_RANGE_FREE_CAM = new ConfigBooleanHotkeyed(
            "fixBeaconRangeFreeCam", true, "",
            "MiniHUD フリーカメラ時のビーコン範囲をプレイヤー位置基準に修正します。",
            "MiniHUD 補正"
    );
    public static final ConfigBooleanHotkeyed DURABILITY_WARNING_ENABLED = new ConfigBooleanHotkeyed(
            "durabilityWarningEnabled", true, "",
            "耐久値が 1% 以下になったときにチャットへ警告を出します。",
            "耐久値警告"
    );
    public static final ConfigBooleanHotkeyed AUTO_RESTOCK_HOTBAR = new ConfigBooleanHotkeyed(
            "autoRestockHotbar", false, "",
            "チェストなどのコンテナを開いた時、設定リストのアイテムをホットバーへ補充します。",
            "ホットバー自動補充"
    );
    public static final ConfigBooleanHotkeyed TOTEM_RESTOCK = new ConfigBooleanHotkeyed(
            "totemRestock", false, "",
            "使用された不死のトーテムをインベントリから探して、使っていた手へ補充します。",
            "トーテム補充"
    );

    // ── リスト ────────────────────────────────────────────
    public static final ConfigStringList HOTBAR_RESTOCK_LIST = new ConfigStringList(
            "hotbarRestockList",
            ImmutableList.of("minecraft:firework_rocket", "minecraft:golden_carrot"),
            "ホットバー自動補充の対象アイテム ID 一覧です。"
    );

    // ── ホットキー ──────────────────────────────────────
    public static final ConfigHotkey OPEN_CONFIG = new ConfigHotkey(
            "openConfig", "RIGHT_SHIFT",
            "HikariTweaks の設定画面を開きます。"
    );
    public static final ConfigHotkey SCOREBOARD_NEXT_PAGE = new ConfigHotkey(
            "scoreboardNextPage", "",
            "スコアボードの次のページへ切り替えます。"
    );
    public static final ConfigHotkey SCOREBOARD_PREV_PAGE = new ConfigHotkey(
            "scoreboardPrevPage", "",
            "スコアボードの前のページへ切り替えます。"
    );

    // ── タブ別リスト ─────────────────────────────────────
    private static final List<IConfigBase> UTILITY = List.of(
            FIX_BEACON_RANGE_FREE_CAM,
            DURABILITY_WARNING_ENABLED,
            AUTO_RESTOCK_HOTBAR,
            TOTEM_RESTOCK
    );
    private static final List<IConfigBase> LISTS = List.of(
            HOTBAR_RESTOCK_LIST
    );
    /** ホットキータブ用 */
    private static final List<IConfigBase> HOTKEYS_LIST = List.of(
            OPEN_CONFIG,
            SCOREBOARD_NEXT_PAGE,
            SCOREBOARD_PREV_PAGE
    );

    static {
        FIX_BEACON_RANGE_FREE_CAM.setValueChangeCallback(c -> onConfigChanged());
        DURABILITY_WARNING_ENABLED.setValueChangeCallback(c -> onConfigChanged());
        AUTO_RESTOCK_HOTBAR.setValueChangeCallback(c -> onConfigChanged());
        TOTEM_RESTOCK.setValueChangeCallback(c -> onConfigChanged());
        HOTBAR_RESTOCK_LIST.setValueChangeCallback(c -> onConfigChanged());
        OPEN_CONFIG.setValueChangeCallback(c -> onConfigChanged());
        SCOREBOARD_NEXT_PAGE.setValueChangeCallback(c -> onConfigChanged());
        SCOREBOARD_PREV_PAGE.setValueChangeCallback(c -> onConfigChanged());
    }

    private TweaksOptions() {}

    public static List<IConfigBase> utility()  { return UTILITY; }
    public static List<IConfigBase> lists()    { return LISTS; }
    public static List<IConfigBase> hotkeys()  { return HOTKEYS_LIST; }

    /** malilib の IHotkey リスト（HotkeyProvider 用） */
    public static List<IHotkey> allHotkeys() {
        return List.of(
                FIX_BEACON_RANGE_FREE_CAM,
                DURABILITY_WARNING_ENABLED,
                AUTO_RESTOCK_HOTBAR,
                TOTEM_RESTOCK,
                OPEN_CONFIG,
                SCOREBOARD_NEXT_PAGE,
                SCOREBOARD_PREV_PAGE
        );
    }

    public static void loadFromConfig(ClientConfig config) {
        config.normalize();
        loadingFromConfig = true;
        try {
            FIX_BEACON_RANGE_FREE_CAM.setBooleanValue(config.fixBeaconRangeFreeCam);
            FIX_BEACON_RANGE_FREE_CAM.getKeybind().setValueFromString(config.fixBeaconRangeFreeCamHotkey);
            DURABILITY_WARNING_ENABLED.setBooleanValue(config.durabilityWarningEnabled);
            DURABILITY_WARNING_ENABLED.getKeybind().setValueFromString(config.durabilityWarningEnabledHotkey);
            AUTO_RESTOCK_HOTBAR.setBooleanValue(config.autoRestockHotbar);
            AUTO_RESTOCK_HOTBAR.getKeybind().setValueFromString(config.autoRestockHotbarHotkey);
            TOTEM_RESTOCK.setBooleanValue(config.totemRestock);
            TOTEM_RESTOCK.getKeybind().setValueFromString(config.totemRestockHotkey);
            HOTBAR_RESTOCK_LIST.setStrings(config.hotbarRestockList);
            OPEN_CONFIG.getKeybind().setValueFromString(config.openConfigHotkey);
            SCOREBOARD_NEXT_PAGE.getKeybind().setValueFromString(config.scoreboardNextPageHotkey);
            SCOREBOARD_PREV_PAGE.getKeybind().setValueFromString(config.scoreboardPrevPageHotkey);
        } finally {
            loadingFromConfig = false;
        }
    }

    public static void writeToConfig(ClientConfig config) {
        config.fixBeaconRangeFreeCam          = FIX_BEACON_RANGE_FREE_CAM.getBooleanValue();
        config.fixBeaconRangeFreeCamHotkey    = FIX_BEACON_RANGE_FREE_CAM.getKeybind().getStringValue();
        config.durabilityWarningEnabled       = DURABILITY_WARNING_ENABLED.getBooleanValue();
        config.durabilityWarningEnabledHotkey = DURABILITY_WARNING_ENABLED.getKeybind().getStringValue();
        config.autoRestockHotbar              = AUTO_RESTOCK_HOTBAR.getBooleanValue();
        config.autoRestockHotbarHotkey        = AUTO_RESTOCK_HOTBAR.getKeybind().getStringValue();
        config.totemRestock                   = TOTEM_RESTOCK.getBooleanValue();
        config.totemRestockHotkey             = TOTEM_RESTOCK.getKeybind().getStringValue();
        config.hotbarRestockList              = new java.util.ArrayList<>(HOTBAR_RESTOCK_LIST.getStrings());
        config.openConfigHotkey               = OPEN_CONFIG.getKeybind().getStringValue();
        config.scoreboardNextPageHotkey       = SCOREBOARD_NEXT_PAGE.getKeybind().getStringValue();
        config.scoreboardPrevPageHotkey       = SCOREBOARD_PREV_PAGE.getKeybind().getStringValue();
        config.normalize();
    }

    public static void applyRuntimeConfig() {
        writeToConfig(ClientConfigManager.config);
    }

    private static void onConfigChanged() {
        if (loadingFromConfig) {
            return;
        }
        if (ClientConfigManager.config != null) {
            applyRuntimeConfig();
            ClientConfigManager.save();
        }
    }
}