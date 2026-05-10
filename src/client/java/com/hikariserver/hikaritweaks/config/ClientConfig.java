package com.hikariserver.hikaritweaks.config;

import java.util.ArrayList;
import java.util.List;

// 設定ファイル（JSON）にシリアライズされるデータクラス。
// フィールドを追加した場合は configVersion を上げ、applyDefaults() に移行処理を追加すること。
public class ClientConfig {

    // ── バージョン管理 ─────────────────────────────────────
    // フィールド追加・削除のたびに +1 する
    public int configVersion = 6;

    // ── 補助機能 ───────────────────────────────────────────
    public boolean fixBeaconRangeFreeCam = true;
    public String  fixBeaconRangeFreeCamHotkey = "";

    public boolean durabilityWarningEnabled = true;
    public String  durabilityWarningEnabledHotkey = "";

    public boolean autoRestockHotbar = false;
    public String  autoRestockHotbarHotkey = "";

    public boolean totemRestock = false;
    public String  totemRestockHotkey = "";

    public boolean autoLitematicaRefresh = false;
    public String  autoLitematicaRefreshHotkey = "";

    // Tweakeroo handrestock 相当：特定アイテムが 5 個以下になったら自動補充
    public boolean handRestock = false;
    public String  handRestockHotkey = "";

    // ── リスト ─────────────────────────────────────────────
    public List<String> hotbarRestockList = new ArrayList<>(List.of(
            "minecraft:firework_rocket",
            "minecraft:golden_carrot"
    ));
    // handRestock の補充対象アイテム ID リスト
    public List<String> handRestockList = new ArrayList<>();

    // ── ホットキー ─────────────────────────────────────────
    public String openConfigHotkey = "RIGHT_SHIFT";
    public String scoreboardNextPageHotkey = "";
    public String scoreboardPrevPageHotkey = "";

    // ── スコアボード表示設定 ───────────────────────────────
    public boolean scoreboardCustomHud      = true;
    public boolean scoreboardHideVanilla    = true;
    public int     scoreboardPageSize       = 10;
    public int     scoreboardPositionX      = 100;
    public int     scoreboardPositionY      = 50;
    public float   scoreboardScale          = 1.0f;
    public int     scoreboardHeaderColor    = 0x66000000;
    public int     scoreboardBodyColor      = 0x4D000000;
    public int     scoreboardTextColor      = 0xFFFFFFFF;
    public int     scoreboardScoreColor     = 0xFFFF5555;
    public int     scoreboardSelfColor      = 0xFFFFFF55;
    public boolean scoreboardShowServerTotal = true;

    // ── アップデートチェッカー ─────────────────────────────
    public boolean updateCheckerEnabled          = true;
    public boolean updateNotifyOnJoin            = true;
    public boolean updateIncludePrerelease        = false;
    public int     updateCheckIntervalMinutes     = 360;
    public String  updateGithubOwner             = "Tamago0314";
    public String  updateGithubRepo              = "Hikari-Tweaks";
    public String  updateReleaseUrlOverride       = "";
    public long    updateLastCheckedAt            = 0L;
    public String  updateLastNotifiedVersion      = "";

    // null ガードと数値の範囲チェックを行う。ロード後に必ず呼ぶこと。
    public void normalize() {
        if (hotbarRestockList == null)      hotbarRestockList = new ArrayList<>();
        if (handRestockList == null)        handRestockList   = new ArrayList<>();
        if (scoreboardNextPageHotkey == null) scoreboardNextPageHotkey = "";
        if (scoreboardPrevPageHotkey == null) scoreboardPrevPageHotkey = "";
        if (autoLitematicaRefreshHotkey == null) autoLitematicaRefreshHotkey = "";
        if (handRestockHotkey == null)      handRestockHotkey = "";
        if (updateGithubOwner == null)      updateGithubOwner = "Tamago0314";
        if (updateGithubRepo == null)       updateGithubRepo  = "Hikari-Tweaks";
        if (updateReleaseUrlOverride == null) updateReleaseUrlOverride = "";
        if (updateLastNotifiedVersion == null) updateLastNotifiedVersion = "";

        scoreboardPageSize  = Math.max(1, Math.min(50, scoreboardPageSize));
        scoreboardPositionX = Math.max(0, Math.min(100, scoreboardPositionX));
        scoreboardPositionY = Math.max(0, Math.min(100, scoreboardPositionY));
        scoreboardScale     = Math.max(0.5f, Math.min(3.0f, scoreboardScale));
        updateCheckIntervalMinutes = Math.max(5, Math.min(7 * 24 * 60, updateCheckIntervalMinutes));

        updateGithubOwner        = updateGithubOwner.trim();
        updateGithubRepo         = updateGithubRepo.trim();
        updateReleaseUrlOverride = updateReleaseUrlOverride.trim();
    }

    // 旧バージョンの設定ファイルを最新スキーマへ段階移行する。
    // configVersion が現在値より低い場合のみ実行される。
    public void applyDefaults() {
        if (configVersion < 1) {
            scoreboardCustomHud   = true;
            scoreboardHideVanilla = true;
            scoreboardPageSize    = scoreboardPageSize  == 0    ? 10        : scoreboardPageSize;
            scoreboardPositionX   = scoreboardPositionX == 0    ? 100       : scoreboardPositionX;
            scoreboardPositionY   = scoreboardPositionY == 0    ? 50        : scoreboardPositionY;
            scoreboardScale       = scoreboardScale      == 0f   ? 1.0f      : scoreboardScale;
            scoreboardHeaderColor = scoreboardHeaderColor == 0  ? 0x66000000 : scoreboardHeaderColor;
            scoreboardBodyColor   = scoreboardBodyColor  == 0   ? 0x4D000000 : scoreboardBodyColor;
            scoreboardTextColor   = scoreboardTextColor  == 0   ? 0xFFFFFFFF : scoreboardTextColor;
            scoreboardScoreColor  = scoreboardScoreColor == 0   ? 0xFFFF5555 : scoreboardScoreColor;
            scoreboardSelfColor   = scoreboardSelfColor  == 0   ? 0xFFFFFF55 : scoreboardSelfColor;
            scoreboardShowServerTotal = true;
            configVersion = 1;
        }
        if (configVersion < 2) {
            updateCheckerEnabled       = true;
            updateNotifyOnJoin         = true;
            updateIncludePrerelease    = false;
            updateCheckIntervalMinutes = 360;
            updateGithubOwner          = "Tamago0314";
            updateGithubRepo           = "Hikari-Tweaks";
            updateReleaseUrlOverride   = "";
            updateLastCheckedAt        = 0L;
            updateLastNotifiedVersion  = "";
            configVersion = 2;
        }
        if (configVersion < 3) {
            scoreboardNextPageHotkey = "";
            scoreboardPrevPageHotkey = "";
            configVersion = 3;
        }
        if (configVersion < 4) {
            // v3 以前のスライダーは 14 段階だったが v4 で 50 段階に拡張。
            // 既存値はそのまま維持（normalize() が上限を担保する）。
            configVersion = 4;
        }
        if (configVersion < 5) {
            autoLitematicaRefresh    = false;
            autoLitematicaRefreshHotkey = "";
            configVersion = 5;
        }
        if (configVersion < 6) {
            handRestock     = false;
            handRestockHotkey = "";
            handRestockList = new ArrayList<>();
            configVersion = 6;
        }
    }
}
