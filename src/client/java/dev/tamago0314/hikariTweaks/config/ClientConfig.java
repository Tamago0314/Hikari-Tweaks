package dev.tamago0314.hikariTweaks.config;

public class ClientConfig {
    // ── バージョン管理 ────────────────────────────────────────
    /** 設定ファイルのスキーマバージョン。フィールド追加時に上げる */
    public int configVersion = 3;

    public boolean fixBeaconRangeFreeCam = true;
    public String fixBeaconRangeFreeCamHotkey = "";
    public boolean durabilityWarningEnabled = true;
    public String durabilityWarningEnabledHotkey = "";
    public boolean autoRestockHotbar = false;
    public String autoRestockHotbarHotkey = "";
    public boolean totemRestock = false;
    public String totemRestockHotkey = "";
    public java.util.List<String> hotbarRestockList = new java.util.ArrayList<>(java.util.List.of("minecraft:firework_rocket", "minecraft:golden_carrot"));
    public String openConfigHotkey = "RIGHT_SHIFT";

    // ── スコアボード表示設定 ──────────────────────────────────────
    /** カスタムHUD描画を使うか（false=バニラのサイドバーをそのまま使う） */
    public boolean scoreboardCustomHud = true;
    /** バニラのサイドバーを非表示にするか */
    public boolean scoreboardHideVanilla = true;
    /** ページサイズ（1ページに表示するエントリ数） */
    public int scoreboardPageSize = 10;
    /** HUD位置 X（0〜100、右端=100） */
    public int scoreboardPositionX = 100;
    /** HUD位置 Y（0〜100、中央=50） */
    public int scoreboardPositionY = 50;
    /** HUD スケール（0.5〜2.0） */
    public float scoreboardScale = 1.0f;
    /** ヘッダー背景色 (ARGB) */
    public int scoreboardHeaderColor = 0x66000000;
    /** 本文背景色 (ARGB) */
    public int scoreboardBodyColor   = 0x4D000000;
    /** テキスト色 (ARGB) */
    public int scoreboardTextColor   = 0xFFFFFFFF;
    /** スコア数値の色 (ARGB) */
    public int scoreboardScoreColor  = 0xFFFF5555;
    /** 自分のエントリの強調色 (ARGB) */
    public int scoreboardSelfColor   = 0xFFFFFF55;
    /** サーバートータルをHUDに表示するか */
    public boolean scoreboardShowServerTotal = true;
    /** 次ページホットキー */
    public String scoreboardNextPageHotkey = "";
    /** 前ページホットキー */
    public String scoreboardPrevPageHotkey = "";

    // update checker
    public boolean updateCheckerEnabled = true;
    public boolean updateNotifyOnJoin = true;
    public boolean updateIncludePrerelease = false;
    public int updateCheckIntervalMinutes = 360;
    public String updateGithubOwner = "Tamago0314";
    public String updateGithubRepo = "Hikari-Tweaks";
    public String updateReleaseUrlOverride = "";
    public long updateLastCheckedAt = 0L;
    public String updateLastNotifiedVersion = "";

    public void normalize() {
        if (hotbarRestockList == null) {
            hotbarRestockList = new java.util.ArrayList<>();
        }
        scoreboardPageSize    = Math.max(1,  Math.min(50, scoreboardPageSize));
        scoreboardPositionX   = Math.max(0,  Math.min(100, scoreboardPositionX));
        scoreboardPositionY   = Math.max(0,  Math.min(100, scoreboardPositionY));
        scoreboardScale       = Math.max(0.5f, Math.min(3.0f, scoreboardScale));
        updateCheckIntervalMinutes = Math.max(5, Math.min(7 * 24 * 60, updateCheckIntervalMinutes));
        if (updateGithubOwner == null) updateGithubOwner = "Tamago0314";
        if (updateGithubRepo == null) updateGithubRepo = "Hikari-Tweaks";
        if (updateReleaseUrlOverride == null) updateReleaseUrlOverride = "";
        if (updateLastNotifiedVersion == null) updateLastNotifiedVersion = "";
        if (scoreboardNextPageHotkey == null) scoreboardNextPageHotkey = "";
        if (scoreboardPrevPageHotkey == null) scoreboardPrevPageHotkey = "";
        updateGithubOwner = updateGithubOwner.trim();
        updateGithubRepo = updateGithubRepo.trim();
        updateReleaseUrlOverride = updateReleaseUrlOverride.trim();
    }

    public void applyDefaults() {
        // configVersion 0 (フィールド自体が無い旧ファイル) → v1 への移行
        if (configVersion < 1) {
            scoreboardCustomHud      = true;
            scoreboardHideVanilla    = true;
            scoreboardPageSize       = scoreboardPageSize == 0 ? 10 : scoreboardPageSize;
            scoreboardPositionX      = scoreboardPositionX == 0 ? 100 : scoreboardPositionX;
            scoreboardPositionY      = scoreboardPositionY == 0 ? 50  : scoreboardPositionY;
            scoreboardScale          = scoreboardScale == 0f ? 1.0f : scoreboardScale;
            scoreboardHeaderColor    = scoreboardHeaderColor == 0 ? 0x66000000 : scoreboardHeaderColor;
            scoreboardBodyColor      = scoreboardBodyColor   == 0 ? 0x4D000000 : scoreboardBodyColor;
            scoreboardTextColor      = scoreboardTextColor   == 0 ? 0xFFFFFFFF : scoreboardTextColor;
            scoreboardScoreColor     = scoreboardScoreColor  == 0 ? 0xFFFF5555 : scoreboardScoreColor;
            scoreboardSelfColor      = scoreboardSelfColor   == 0 ? 0xFFFFFF55 : scoreboardSelfColor;
            scoreboardShowServerTotal = true;
            configVersion = 1;
        }
        if (configVersion < 2) {
            updateCheckerEnabled = true;
            updateNotifyOnJoin = true;
            updateIncludePrerelease = false;
            updateCheckIntervalMinutes = 360;
            updateGithubOwner = "Tamago0314";
            updateGithubRepo = "Hikari-Tweaks";
            updateReleaseUrlOverride = "";
            updateLastCheckedAt = 0L;
            updateLastNotifiedVersion = "";
            configVersion = 2;
        }
        if (configVersion < 3) {
            scoreboardNextPageHotkey = "";
            scoreboardPrevPageHotkey = "";
            configVersion = 3;
        }
    }
}