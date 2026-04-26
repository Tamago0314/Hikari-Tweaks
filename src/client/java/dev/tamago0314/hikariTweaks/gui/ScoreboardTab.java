package dev.tamago0314.hikariTweaks.gui;

import dev.tamago0314.hikariTweaks.config.ClientConfig;
import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import dev.tamago0314.hikariTweaks.scoreboard.PlayerListEntry;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardHudRenderer;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardPacketClient;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISliderCallback;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Hikari-Tweaks 設定画面の「スコアボード」タブ。
 *
 * ボタン／スライダーはすべて malilib の ButtonGeneric／WidgetSlider を使用し、
 * HikariTweaksConfigScreen の addButton()／addWidget() に登録する。
 * これにより malilib の onMouseClicked ループだけがイベントを処理し、
 * バニラ Screen.mouseClicked() によるクリック音の二重再生を防ぐ。
 *
 * malilib 0.12.0 には setVisible() が存在しないため、
 * 非アクティブタブのボタンは y = OFFSCREEN（-2000）に退避して実質的に非表示にする。
 */
public final class ScoreboardTab {

    private enum SubTab {
        PLAYERS("プレイヤー"), DISPLAY("表示設定");
        final String label;
        SubTab(String l) { label = l; }
    }

    /** ボタン／ウィジェットを malilib の GUI に登録するためのインターフェース */
    public interface WidgetHost {
        <T extends ButtonBase>  T addButton(T button, IButtonActionListener listener);
        <T extends WidgetBase>  T addWidget(T widget);
    }

    // ───── 定数 ─────
    private static final int ROW_HEIGHT  = 20;
    private static final int LIST_X      = 10;
    private static final int BUTTON_W    = 70;
    private static final int SCROLL_SPEED = 4;
    private static final int SCROLLBAR_W = 8;
    private static final int SUBTAB_H    = 16;
    /** 非アクティブタブのボタンを退避するスクリーン外 Y 座標 */
    private static final int OFFSCREEN   = -2000;

    // ───── 状態 ─────
    private final Screen     parent;
    private final WidgetHost host;
    private SubTab activeSubTab = SubTab.PLAYERS;

    private int x, y, width, height;

    // PLAYERS サブタブ
    private List<PlayerListEntry> entries = new ArrayList<>();
    private int     scrollOffset  = 0;
    // FIX③: loaded を init() のたびにリセットするため、onClose() でもリセットする
    private boolean loaded        = false;
    private boolean waiting       = false;
    private boolean draggingScrollbar = false;
    private double  dragStartMouseY   = 0;
    private int     dragStartScroll   = 0;

    // ───── malilib ボタン／ウィジェット ─────
    // サブタブボタンは malilib に登録せず render()/mouseClicked() で自前描画・判定する

    // プレイヤータブ専用
    // FIX①: ButtonGeneric → HideableButton に変更（setShown() が呼べるようにする）
    private HideableButton refreshBtn;
    private final List<HideableButton> rowButtons = new ArrayList<>();

    // 表示設定タブ専用
    private HideableButton customHudBtn;
    private HideableButton hideVanillaBtn;
    private HideableButton showTotalBtn;
    private HideableSlider pageSizeSlider;
    private HideableButton prevPageBtn;
    private HideableButton nextPageBtn;
    private HideableButton resetPageBtn;
    private HideableButton posEditorBtn;
    private HideableButton colorPickerBtn;
    private HideableSlider scaleSlider;

    // ───── コンストラクタ ─────

    public ScoreboardTab(Screen parent, WidgetHost host) {
        this.parent = parent;
        this.host   = host;
    }

    // ───── 初期化 ─────

    public void init(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;

        // FIX③: init() が呼ばれるたびに loaded をリセットし、再オープン時も再取得を行う
        loaded = false;

        // ── プレイヤータブ ──
        // FIX①: new ButtonGeneric → new HideableButton に変更
        refreshBtn = host.addButton(
                new HideableButton(x + width - BUTTON_W - 4, y + SUBTAB_H + 4, BUTTON_W, 16, "Refresh"),
                (btn, mb) -> requestList());

        // ── 表示設定タブ ──
        ClientConfig cfg = ClientConfigManager.config;
        int lx = x + 8;

        customHudBtn = host.addButton(new HideableButton(
                        lx, dispRowY(0), 200, ROW_HEIGHT,
                        "カスタムHUD描画: " + onOff(cfg.scoreboardCustomHud)),
                (btn, mb) -> {
                    cfg.scoreboardCustomHud = !cfg.scoreboardCustomHud;
                    ClientConfigManager.save();
                    btn.setDisplayString("カスタムHUD描画: " + onOff(cfg.scoreboardCustomHud));
                });

        hideVanillaBtn = host.addButton(new HideableButton(
                        lx, dispRowY(1), 200, ROW_HEIGHT,
                        "バニラサイドバー非表示: " + onOff(cfg.scoreboardHideVanilla)),
                (btn, mb) -> {
                    cfg.scoreboardHideVanilla = !cfg.scoreboardHideVanilla;
                    ClientConfigManager.save();
                    btn.setDisplayString("バニラサイドバー非表示: " + onOff(cfg.scoreboardHideVanilla));
                });

        showTotalBtn = host.addButton(new HideableButton(
                        lx, dispRowY(2), 200, ROW_HEIGHT,
                        "サーバートータル表示: " + onOff(cfg.scoreboardShowServerTotal)),
                (btn, mb) -> {
                    cfg.scoreboardShowServerTotal = !cfg.scoreboardShowServerTotal;
                    ClientConfigManager.save();
                    btn.setDisplayString("サーバートータル表示: " + onOff(cfg.scoreboardShowServerTotal));
                });

        pageSizeSlider = host.addWidget(
                new HideableSlider(lx + 100, dispRowY(3), 120, ROW_HEIGHT,
                        new PageSizeCallback(cfg)));

        prevPageBtn = host.addButton(new HideableButton(
                        lx,       dispRowY(4), 58, 16, "◀ 前ページ"),
                (btn, mb) -> ScoreboardHudRenderer.prevPage());

        nextPageBtn = host.addButton(new HideableButton(
                        lx + 62,  dispRowY(4), 58, 16, "次ページ ▶"),
                (btn, mb) -> ScoreboardHudRenderer.nextPage());

        resetPageBtn = host.addButton(new HideableButton(
                        lx + 124, dispRowY(4), 48, 16, "先頭へ"),
                (btn, mb) -> ScoreboardHudRenderer.resetPage());

        posEditorBtn = host.addButton(new HideableButton(
                        lx,       dispRowY(5), 126, 18, "位置調整画面を開く"),
                (btn, mb) -> MinecraftClient.getInstance().setScreen(new PositionEditorScreen(parent)));

        colorPickerBtn = host.addButton(new HideableButton(
                        lx + 130, dispRowY(5), 110, 18, "色設定画面を開く"),
                (btn, mb) -> MinecraftClient.getInstance().setScreen(new ColorPickerScreen(parent)));

        scaleSlider = host.addWidget(
                new HideableSlider(lx + 60, dispRowY(6), 160, ROW_HEIGHT,
                        new ScaleCallback(cfg)));

        // ── サーバーコールバック ──
        ScoreboardPacketClient.setOnListUpdated(list -> {
            this.entries = new ArrayList<>(list);
            this.scrollOffset = 0;
            this.waiting = false;
            rebuildRowButtons();
            // FIX②: コールバック経由で rowButtons が再構築された後、
            //        現在のサブタブに合わせて表示状態を正しく反映する
            updateSubTabVisibility();
        });
        ScoreboardPacketClient.setOnRankingUpdated(ScoreboardHudRenderer::resetPage);

        if (!loaded) {
            loaded = true;
            List<PlayerListEntry> cached = ScoreboardPacketClient.getCachedList();
            if (!cached.isEmpty()) {
                entries = new ArrayList<>(cached);
                rebuildRowButtons();
            } else {
                requestList();
            }
        }

        updateSubTabVisibility();
    }

    public void onClose() {
        ScoreboardPacketClient.setOnListUpdated(null);
        ScoreboardPacketClient.setOnRankingUpdated(null);
        // FIX③: 画面を閉じたら loaded をリセットし、次回オープン時に再取得させる
        loaded = false;
    }

    // ───── サブタブ切り替え ─────

    private void switchSubTab(SubTab tab) {
        activeSubTab = tab;
        updateSubTabVisibility();
    }

    /**
     * アクティブなサブタブに応じてボタン／ウィジェットを表示・退避する。
     * setVisible() が malilib 0.12.0 にないため、非アクティブ側は OFFSCREEN へ退避。
     */
    private void updateSubTabVisibility() {
        boolean players = activeSubTab == SubTab.PLAYERS;

        // サブタブボタンは自前描画のため malilib 側の操作不要

        // プレイヤータブ
        setShown(refreshBtn, players);
        for (HideableButton b : rowButtons) setShown(b, players);

        // 表示設定タブ
        boolean display = !players;
        setShown(customHudBtn,   display);
        setShown(hideVanillaBtn, display);
        setShown(showTotalBtn,   display);
        if (pageSizeSlider != null) pageSizeSlider.setShown(display);
        if (scaleSlider    != null) scaleSlider.setShown(display);
        setShown(prevPageBtn,    display);
        setShown(nextPageBtn,    display);
        setShown(resetPageBtn,   display);
        setShown(posEditorBtn,   display);
        setShown(colorPickerBtn, display);
    }

    private static void setShown(HideableButton btn, boolean shown) {
        if (btn != null) btn.setShown(shown);
    }

    // ───── 行ボタン再構築 ─────

    private void rebuildRowButtons() {
        // 古い行ボタンを退避
        for (HideableButton b : rowButtons) b.setShown(false);
        rowButtons.clear();

        if (activeSubTab != SubTab.PLAYERS) return;

        for (PlayerListEntry entry : entries) {
            HideableButton btn = host.addButton(
                    new HideableButton(LIST_X, OFFSCREEN, BUTTON_W, ROW_HEIGHT - 2,
                            entry.isBlocked() ? "§c非表示" : "§a表示中"),
                    (b, mb) -> {
                        ScoreboardPacketClient.toggleBlock(entry.uuid());
                        waiting = true;
                    });
            btn.setShown(true); // プレイヤータブがアクティブなら表示位置は render で更新
            rowButtons.add(btn);
        }
    }

    // ───── 描画 ─────

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // サブタブボタンを自前描画（背景＋テキスト）
        renderSubTabButton(matrices, tr, x,      y, 90, SUBTAB_H, SubTab.PLAYERS, mouseX, mouseY);
        renderSubTabButton(matrices, tr, x + 94, y, 90, SUBTAB_H, SubTab.DISPLAY, mouseX, mouseY);
        Screen.fill(matrices, x, y + SUBTAB_H + 2, x + width, y + SUBTAB_H + 3, 0x66FFFFFF);

        if (activeSubTab == SubTab.PLAYERS) {
            renderPlayersContent(matrices, tr, mouseX, mouseY);
        } else {
            renderDisplayContent(matrices, tr);
        }
    }

    private void renderSubTabButton(MatrixStack m, TextRenderer tr,
                                    int bx, int by, int bw, int bh, SubTab tab, int mouseX, int mouseY) {
        boolean active  = (activeSubTab == tab);
        boolean hovered = !active && mouseX >= bx && mouseX < bx + bw
                && mouseY >= by && mouseY < by + bh;
        int bg = active ? 0xFF555555 : hovered ? 0xFF3A3A5A : 0xFF222244;
        Screen.fill(m, bx, by, bx + bw, by + bh, bg);
        // アクティブタブは上辺を明るく、非アクティブは暗め
        Screen.fill(m, bx, by, bx + bw, by + 1, active ? 0xFFFFFFFF : 0x88FFFFFF);
        // ラベルを中央に描画
        int textColor = active ? 0xFFFFFF55 : 0xFFCCCCCC;
        float tx = bx + (bw - tr.getWidth(tab.label)) / 2f;
        float ty = by + (bh - 8) / 2f;
        tr.drawWithShadow(m, tab.label, tx, ty, textColor);
    }

    private void renderPlayersContent(MatrixStack matrices, TextRenderer tr, int mouseX, int mouseY) {
        int headerY = y + SUBTAB_H + 4;
        tr.drawWithShadow(matrices, "§e表示名", LIST_X + BUTTON_W + 4,   headerY + 4, 0xFFFFFF);
        tr.drawWithShadow(matrices, "§7種別",   LIST_X + BUTTON_W + 130, headerY + 4, 0xFFFFFF);

        if (waiting) {
            drawCentered(matrices, tr, "§7サーバーから取得中...", x + width / 2, y + height / 2, 0xAAAAAA);
            return;
        }
        if (entries.isEmpty()) {
            drawCentered(matrices, tr, "§7プレイヤーデータなし", x + width / 2, y + height / 2, 0xAAAAAA);
            return;
        }

        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, entries.size() * ROW_HEIGHT - visibleH);
        scrollOffset   = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Scissor でリスト領域を切り抜く
        int listWidth = width - SCROLLBAR_W - 4;
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(
                (int)(x * scale),
                (int)(MinecraftClient.getInstance().getWindow().getHeight() - listBottom * scale),
                (int)(listWidth * scale), (int)(visibleH * scale));

        int drawY = listTop - scrollOffset;
        for (int i = 0; i < entries.size(); i++) {
            PlayerListEntry entry = entries.get(i);
            int rowY = drawY + i * ROW_HEIGHT;
            boolean inView = rowY + ROW_HEIGHT >= listTop && rowY <= listBottom;

            // 行ボタンは常に更新する（範囲外は OFFSCREEN に退避して残像を防ぐ）
            if (i < rowButtons.size()) {
                HideableButton btn = rowButtons.get(i);
                if (inView) {
                    btn.setY(rowY + 1);
                    btn.setDisplayString(entry.isBlocked() ? "§c非表示" : "§a表示中");
                } else {
                    btn.setY(OFFSCREEN);
                }
            }

            if (!inView) continue;

            if (i % 2 == 0) Screen.fill(matrices, x, rowY, x + width, rowY + ROW_HEIGHT, 0x18FFFFFF);

            int nc = entry.isBlocked() ? 0x666666 : 0xFFFFFF;
            String name = entry.displayName().length() > 20
                    ? entry.displayName().substring(0, 19) + "…" : entry.displayName();
            tr.drawWithShadow(matrices, name,
                    LIST_X + BUTTON_W + 4, rowY + 6, nc);
            tr.drawWithShadow(matrices, entry.isBot() ? "§6[Bot]" : "§7[人]",
                    LIST_X + BUTTON_W + 130, rowY + 6, 0xFFFFFF);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();

        // スクロールバー
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / (entries.size() * ROW_HEIGHT));
            barH       = Math.min(barH, visibleH);
            int trackH = visibleH - barH;
            int barY   = listTop + (trackH > 0 ? trackH * scrollOffset / maxScroll : 0);
            barY       = Math.min(barY, listBottom - barH);
            int barX   = x + width - SCROLLBAR_W - 2;
            boolean hov = mouseX >= barX && mouseX <= barX + SCROLLBAR_W
                    && mouseY >= barY && mouseY <= barY + barH;
            Screen.fill(matrices, barX, listTop,  barX + SCROLLBAR_W, listBottom, 0x55000000);
            Screen.fill(matrices, barX, barY, barX + SCROLLBAR_W, barY + barH,
                    draggingScrollbar ? 0xFFFFFFFF : hov ? 0xCCFFFFFF : 0x99AAAAAA);
        }
    }

    private void renderDisplayContent(MatrixStack matrices, TextRenderer tr) {
        ClientConfig cfg = ClientConfigManager.config;
        int lx = x + 8;

        // ページサイズラベル（スライダーの左）
        tr.drawWithShadow(matrices, "ページサイズ:", lx, dispRowY(3) + 4, 0xFFFFFF);

        // 現在ページ情報
        int infoY = dispRowY(3) + ROW_HEIGHT + 4;
        ScoreboardPacketClient.RankingData data = ScoreboardPacketClient.getCachedRanking();
        int total = data != null ? data.full().size() : 0;
        int page  = ScoreboardHudRenderer.getCurrentPage();
        int maxP  = ScoreboardHudRenderer.getMaxPage(total, cfg.scoreboardPageSize);
        tr.drawWithShadow(matrices,
                "ランキング: " + total + "人   ページ: " + (page + 1) + "/" + (maxP + 1),
                lx, infoY, 0xAAAAAA);

        // 仕切り線
        int divY = dispRowY(4) + 22;
        Screen.fill(matrices, x, divY, x + width, divY + 1, 0x44FFFFFF);

        // スケールスライダーのラベル
        tr.drawWithShadow(matrices, "スケール:", lx, dispRowY(6) + 4, 0xFFFFFF);
        // 位置情報
        tr.drawWithShadow(matrices,
                String.format("X:%d%%  Y:%d%%",
                        cfg.scoreboardPositionX, cfg.scoreboardPositionY),
                lx, dispRowY(6) + 26, 0xAAAAAA);
    }

    // ───── 座標ヘルパー ─────

    /**
     * 表示設定タブの行 Y 座標を一元管理する。
     * init() と render() の両方がこのメソッドのみを参照するため座標ズレが起きない。
     */
    private int dispRowY(int index) {
        int base = y + SUBTAB_H + 8;
        return switch (index) {
            case 0 -> base;                                              // カスタムHUD
            case 1 -> base + ROW_HEIGHT;                                // バニラ非表示
            case 2 -> base + ROW_HEIGHT * 2;                            // サーバートータル
            case 3 -> base + ROW_HEIGHT * 3;                            // ページサイズスライダー
            case 4 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12;     // ページ操作ボタン
            case 5 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12 + 22 + 1 + 6; // 位置/色ボタン
            case 6 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12 + 22 + 1 + 6 + 24; // スケールスライダー
            default -> base;
        };
    }

    // ───── 入力処理（スクロールバーのみ、ボタンは malilib が処理） ─────

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // サブタブボタンのクリック判定（自前）
            if (my >= y && my < y + SUBTAB_H) {
                if (mx >= x && mx < x + 90 && activeSubTab != SubTab.PLAYERS) {
                    switchSubTab(SubTab.PLAYERS);
                    return true;
                }
                if (mx >= x + 94 && mx < x + 94 + 90 && activeSubTab != SubTab.DISPLAY) {
                    switchSubTab(SubTab.DISPLAY);
                    return true;
                }
            }
        }
        if (button != 0 || activeSubTab != SubTab.PLAYERS) return false;

        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, entries.size() * ROW_HEIGHT - visibleH);
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / (entries.size() * ROW_HEIGHT));
            barH       = Math.min(barH, visibleH);
            int trackH = visibleH - barH;
            int barY   = listTop + (trackH > 0 ? trackH * scrollOffset / maxScroll : 0);
            barY       = Math.min(barY, listBottom - barH);
            int barX   = x + width - SCROLLBAR_W - 2;
            if (mx >= barX && mx <= barX + SCROLLBAR_W && my >= listTop && my <= listBottom) {
                draggingScrollbar = true;
                dragStartMouseY   = my;
                dragStartScroll   = scrollOffset;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingScrollbar) { draggingScrollbar = false; return true; }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!draggingScrollbar) return false;
        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, entries.size() * ROW_HEIGHT - visibleH);
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / (entries.size() * ROW_HEIGHT));
            barH       = Math.min(barH, visibleH);
            int trackH = visibleH - barH;
            if (trackH > 0) {
                scrollOffset = Math.max(0, Math.min(
                        dragStartScroll + (int)((my - dragStartMouseY) * maxScroll / trackH),
                        maxScroll));
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (activeSubTab != SubTab.PLAYERS) return false;
        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        if (my < listTop || my > listBottom) return false;
        int visibleH  = listBottom - listTop;
        int maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - visibleH);
        scrollOffset  = Math.max(0, Math.min((int)(scrollOffset - amount * SCROLL_SPEED), maxScroll));
        return true;
    }

    // ───── ヘルパー ─────

    private void requestList() { waiting = true; ScoreboardPacketClient.requestPlayerList(); }

    private static String onOff(boolean v) { return v ? "§aON" : "§cOFF"; }

    private static void drawCentered(MatrixStack m, TextRenderer tr, String text, int cx, int cy, int color) {
        tr.drawWithShadow(m, text, cx - tr.getWidth(text) / 2f, cy, color);
    }

    // ───── ISliderCallback 実装（スケール 0.5–3.0） ─────

    private static class ScaleCallback implements ISliderCallback {
        private final ClientConfig cfg;

        ScaleCallback(ClientConfig cfg) { this.cfg = cfg; }

        @Override public int getMaxSteps() { return 50; }

        @Override
        public double getValueRelative() {
            // 0.5-3.0 → 0.0-1.0 (刻み 0.05 × 50 ステップ)
            return (cfg.scoreboardScale - 0.5f) / 2.5f;
        }

        @Override
        public void setValueRelative(double rel) {
            // 0.0-1.0 → 0.5-3.0、0.05刻みに丸める
            float v = (float)(Math.round(rel * 50) * 0.05 + 0.5);
            v = Math.max(0.5f, Math.min(3.0f, v));
            if (Math.abs(cfg.scoreboardScale - v) > 0.001f) {
                cfg.scoreboardScale = v;
                ClientConfigManager.save();
            }
        }

        @Override
        public String getFormattedDisplayValue() {
            return String.format("%.2fx", cfg.scoreboardScale);
        }
    }

    // ───── 内部クラス：表示切り替え可能な ButtonGeneric ─────

    /**
     * malilib 0.12.0 には setVisible() がないため、
     * 非表示時は y を OFFSCREEN に退避することで isMouseOver() が常に false になるようにする。
     * 描画も malilib 内部の visible フィールドで制御する。
     */
    public static class HideableButton extends ButtonGeneric {
        private int shownY = OFFSCREEN;

        public HideableButton(int x, int y, int width, int height, String text) {
            super(x, OFFSCREEN, width, height, text);
            this.shownY = y;
        }

        public void setShown(boolean shown) {
            this.y       = shown ? shownY : OFFSCREEN;
            this.visible = shown;
            this.enabled = shown;
        }

        /** 表示位置の Y を更新する（スクロール追従など） */
        public void setShownY(int y) {
            this.shownY = y;
            if (this.visible) this.y = y;
        }
    }

    // ───── 内部クラス：表示切り替え可能な WidgetSlider ─────

    public static class HideableSlider extends WidgetSlider {
        private int shownY;

        public HideableSlider(int x, int y, int width, int height, ISliderCallback callback) {
            super(x, OFFSCREEN, width, height, callback);
            this.shownY = y;
        }

        public void setShown(boolean shown) {
            this.y = shown ? shownY : OFFSCREEN;
        }
    }

    // ───── ISliderCallback 実装（ページサイズ 1–50） ─────

    private static class PageSizeCallback implements ISliderCallback {
        private final ClientConfig cfg;

        PageSizeCallback(ClientConfig cfg) { this.cfg = cfg; }

        @Override public int getMaxSteps() { return 49; }

        @Override
        public double getValueRelative() {
            return (cfg.scoreboardPageSize - 1) / 49.0;
        }

        @Override
        public void setValueRelative(double rel) {
            int v = Math.max(1, Math.min(50, (int) Math.round(rel * 49) + 1));
            if (cfg.scoreboardPageSize != v) {
                cfg.scoreboardPageSize = v;
                ClientConfigManager.save();
                ScoreboardHudRenderer.resetPage();
            }
        }

        @Override
        public String getFormattedDisplayValue() {
            return "ページサイズ: " + cfg.scoreboardPageSize;
        }
    }
}