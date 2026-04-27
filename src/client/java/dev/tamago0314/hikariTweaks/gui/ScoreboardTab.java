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
import java.util.stream.Collectors;

/**
 * Hikari-Tweaks 設定画面の「スコアボード」タブ。
 */
public final class ScoreboardTab {

    private enum SubTab {
        PLAYERS("プレイヤー"), DISPLAY("表示設定");
        final String label;
        SubTab(String l) { label = l; }
    }

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
    private static final int OFFSCREEN   = -2000;
    /** カテゴリヘッダーの高さ */
    private static final int CATEGORY_H  = 14;

    // ───── 状態 ─────
    private final Screen     parent;
    private final WidgetHost host;
    private SubTab activeSubTab = SubTab.PLAYERS;

    private int x, y, width, height;

    // PLAYERS サブタブ
    private List<PlayerListEntry> entries = new ArrayList<>();
    private int     scrollOffset  = 0;
    private boolean loaded        = false;
    private boolean waiting       = false;
    private boolean draggingScrollbar = false;
    private double  dragStartMouseY   = 0;
    private int     dragStartScroll   = 0;

    // ───── malilib ボタン／ウィジェット ─────
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

        loaded = false;

        // ── プレイヤータブ ──
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
            // スクロール位置を保持するためリセットしない
            this.entries = new ArrayList<>(list);
            this.waiting = false;
            rebuildRowButtons();
            updateSubTabVisibility();
        });
        // ランキング更新時はページをリセットしない（現在ページを維持）
        ScoreboardPacketClient.setOnRankingUpdated(() -> {
            // ページの clamp は ScoreboardHudRenderer.render() 内で行うため何もしない
        });

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
        loaded = false;
    }

    // ───── サブタブ切り替え ─────

    private void switchSubTab(SubTab tab) {
        activeSubTab = tab;
        updateSubTabVisibility();
    }

    private void updateSubTabVisibility() {
        boolean players = activeSubTab == SubTab.PLAYERS;

        setShown(refreshBtn, players);
        for (HideableButton b : rowButtons) setShown(b, players);

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
        for (HideableButton b : rowButtons) b.setShown(false);
        rowButtons.clear();

        if (activeSubTab != SubTab.PLAYERS) return;

        // 表示中と非表示でカテゴリ分け
        List<PlayerListEntry> visible  = entries.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList());
        List<PlayerListEntry> blocked  = entries.stream().filter(PlayerListEntry::isBlocked).collect(Collectors.toList());

        // 表示中カテゴリ
        // カテゴリヘッダーはボタンではなく render() 側で描画するので、
        // rowButtons にはエントリ数だけ登録し、描画時にオフセット計算する
        addEntryButtons(visible);
        addEntryButtons(blocked);
    }

    private void addEntryButtons(List<PlayerListEntry> list) {
        for (PlayerListEntry entry : list) {
            HideableButton btn = host.addButton(
                    new HideableButton(LIST_X, OFFSCREEN, BUTTON_W, ROW_HEIGHT - 2,
                            entry.isBlocked() ? "§c非表示" : "§a表示中"),
                    (b, mb) -> {
                        ScoreboardPacketClient.toggleBlock(entry.uuid());
                        waiting = true;
                    });
            btn.setShown(true);
            rowButtons.add(btn);
        }
    }

    // ───── 描画 ─────

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

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
        Screen.fill(m, bx, by, bx + bw, by + 1, active ? 0xFFFFFFFF : 0x88FFFFFF);
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

        // カテゴリ分け
        List<PlayerListEntry> visibleEntries = entries.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList());
        List<PlayerListEntry> blockedEntries = entries.stream().filter(PlayerListEntry::isBlocked).collect(Collectors.toList());

        // 仮想行リスト（カテゴリヘッダー込み）を構築してスクロール計算に使う
        int totalVirtualH = 0;
        if (!visibleEntries.isEmpty()) totalVirtualH += CATEGORY_H + visibleEntries.size() * ROW_HEIGHT;
        if (!blockedEntries.isEmpty()) totalVirtualH += CATEGORY_H + blockedEntries.size() * ROW_HEIGHT;

        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, totalVirtualH - visibleH);
        scrollOffset   = Math.max(0, Math.min(scrollOffset, maxScroll));

        int listWidth = width - SCROLLBAR_W - 4;
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(
                (int)(x * scale),
                (int)(MinecraftClient.getInstance().getWindow().getHeight() - listBottom * scale),
                (int)(listWidth * scale), (int)(visibleH * scale));

        int drawY = listTop - scrollOffset;
        int rowBtnIndex = 0;

        // ── 表示中カテゴリ ──
        if (!visibleEntries.isEmpty()) {
            int catY = drawY;
            if (catY + CATEGORY_H >= listTop && catY <= listBottom) {
                Screen.fill(matrices, x, catY, x + width, catY + CATEGORY_H, 0x44AAFFAA);
                tr.drawWithShadow(matrices, "§a■ 表示中 (" + visibleEntries.size() + ")", LIST_X + 2, catY + 3, 0xAAFFAA);
            }
            drawY += CATEGORY_H;

            for (int i = 0; i < visibleEntries.size(); i++) {
                PlayerListEntry entry = visibleEntries.get(i);
                int rowY = drawY + i * ROW_HEIGHT;
                boolean inView = rowY + ROW_HEIGHT >= listTop && rowY <= listBottom;

                if (rowBtnIndex < rowButtons.size()) {
                    HideableButton btn = rowButtons.get(rowBtnIndex);
                    if (inView) {
                        btn.setY(rowY + 1);
                        btn.setDisplayString("§a表示中");
                    } else {
                        btn.setY(OFFSCREEN);
                    }
                }
                rowBtnIndex++;

                if (!inView) continue;
                if (i % 2 == 0) Screen.fill(matrices, x, rowY, x + width, rowY + ROW_HEIGHT, 0x18FFFFFF);
                String name = truncate(entry.displayName(), 20);
                tr.drawWithShadow(matrices, name, LIST_X + BUTTON_W + 4, rowY + 6, 0xFFFFFF);
                tr.drawWithShadow(matrices, entry.isBot() ? "§6[Bot]" : "§7[人]", LIST_X + BUTTON_W + 130, rowY + 6, 0xFFFFFF);
            }
            drawY += visibleEntries.size() * ROW_HEIGHT;
        }

        // ── 非表示カテゴリ ──
        if (!blockedEntries.isEmpty()) {
            int catY = drawY;
            if (catY + CATEGORY_H >= listTop && catY <= listBottom) {
                Screen.fill(matrices, x, catY, x + width, catY + CATEGORY_H, 0x44FF8888);
                tr.drawWithShadow(matrices, "§c■ 非表示 (" + blockedEntries.size() + ")", LIST_X + 2, catY + 3, 0xFFAAAA);
            }
            drawY += CATEGORY_H;

            for (int i = 0; i < blockedEntries.size(); i++) {
                PlayerListEntry entry = blockedEntries.get(i);
                int rowY = drawY + i * ROW_HEIGHT;
                boolean inView = rowY + ROW_HEIGHT >= listTop && rowY <= listBottom;

                if (rowBtnIndex < rowButtons.size()) {
                    HideableButton btn = rowButtons.get(rowBtnIndex);
                    if (inView) {
                        btn.setY(rowY + 1);
                        btn.setDisplayString("§c非表示");
                    } else {
                        btn.setY(OFFSCREEN);
                    }
                }
                rowBtnIndex++;

                if (!inView) continue;
                if (i % 2 == 0) Screen.fill(matrices, x, rowY, x + width, rowY + ROW_HEIGHT, 0x18FFFFFF);
                String name = truncate(entry.displayName(), 20);
                tr.drawWithShadow(matrices, name, LIST_X + BUTTON_W + 4, rowY + 6, 0x666666);
                tr.drawWithShadow(matrices, entry.isBot() ? "§6[Bot]" : "§7[人]", LIST_X + BUTTON_W + 130, rowY + 6, 0xFFFFFF);
            }
        }

        // 退避（残像防止）
        while (rowBtnIndex < rowButtons.size()) {
            rowButtons.get(rowBtnIndex).setY(OFFSCREEN);
            rowBtnIndex++;
        }

        com.mojang.blaze3d.systems.RenderSystem.disableScissor();

        // スクロールバー
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / Math.max(1, totalVirtualH));
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

        tr.drawWithShadow(matrices, "ページサイズ:", lx, dispRowY(3) + 4, 0xFFFFFF);

        int infoY = dispRowY(3) + ROW_HEIGHT + 4;
        ScoreboardPacketClient.RankingData data = ScoreboardPacketClient.getCachedRanking();
        int total = data != null ? data.full().size() : 0;
        int page  = ScoreboardHudRenderer.getCurrentPage();
        int maxP  = ScoreboardHudRenderer.getMaxPage(total, cfg.scoreboardPageSize);
        tr.drawWithShadow(matrices,
                "ランキング: " + total + "人   ページ: " + (page + 1) + "/" + (maxP + 1),
                lx, infoY, 0xAAAAAA);

        int divY = dispRowY(4) + 22;
        Screen.fill(matrices, x, divY, x + width, divY + 1, 0x44FFFFFF);

        tr.drawWithShadow(matrices, "スケール:", lx, dispRowY(6) + 4, 0xFFFFFF);
        tr.drawWithShadow(matrices,
                String.format("X:%d%%  Y:%d%%",
                        cfg.scoreboardPositionX, cfg.scoreboardPositionY),
                lx, dispRowY(6) + 26, 0xAAAAAA);
    }

    // ───── 座標ヘルパー ─────

    private int dispRowY(int index) {
        int base = y + SUBTAB_H + 8;
        return switch (index) {
            case 0 -> base;
            case 1 -> base + ROW_HEIGHT;
            case 2 -> base + ROW_HEIGHT * 2;
            case 3 -> base + ROW_HEIGHT * 3;
            case 4 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12;
            case 5 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12 + 22 + 1 + 6;
            case 6 -> base + ROW_HEIGHT * 3 + ROW_HEIGHT + 4 + 12 + 22 + 1 + 6 + 24;
            default -> base;
        };
    }

    // ───── 入力処理 ─────

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
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

        // カテゴリ込みの totalVirtualH を再計算
        List<PlayerListEntry> visibleEntries = entries.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList());
        List<PlayerListEntry> blockedEntries = entries.stream().filter(PlayerListEntry::isBlocked).collect(Collectors.toList());
        int totalVirtualH = 0;
        if (!visibleEntries.isEmpty()) totalVirtualH += CATEGORY_H + visibleEntries.size() * ROW_HEIGHT;
        if (!blockedEntries.isEmpty()) totalVirtualH += CATEGORY_H + blockedEntries.size() * ROW_HEIGHT;

        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, totalVirtualH - visibleH);
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / Math.max(1, totalVirtualH));
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
        List<PlayerListEntry> visibleEntries = entries.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList());
        List<PlayerListEntry> blockedEntries = entries.stream().filter(PlayerListEntry::isBlocked).collect(Collectors.toList());
        int totalVirtualH = 0;
        if (!visibleEntries.isEmpty()) totalVirtualH += CATEGORY_H + visibleEntries.size() * ROW_HEIGHT;
        if (!blockedEntries.isEmpty()) totalVirtualH += CATEGORY_H + blockedEntries.size() * ROW_HEIGHT;

        int listTop    = y + SUBTAB_H + ROW_HEIGHT + 4;
        int listBottom = y + height - 4;
        int visibleH   = listBottom - listTop;
        int maxScroll  = Math.max(0, totalVirtualH - visibleH);
        if (maxScroll > 0) {
            int barH   = Math.max(24, visibleH * visibleH / Math.max(1, totalVirtualH));
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
        List<PlayerListEntry> visibleEntries = entries.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList());
        List<PlayerListEntry> blockedEntries = entries.stream().filter(PlayerListEntry::isBlocked).collect(Collectors.toList());
        int totalVirtualH = 0;
        if (!visibleEntries.isEmpty()) totalVirtualH += CATEGORY_H + visibleEntries.size() * ROW_HEIGHT;
        if (!blockedEntries.isEmpty()) totalVirtualH += CATEGORY_H + blockedEntries.size() * ROW_HEIGHT;
        int visibleH  = listBottom - listTop;
        int maxScroll = Math.max(0, totalVirtualH - visibleH);
        scrollOffset  = Math.max(0, Math.min((int)(scrollOffset - amount * SCROLL_SPEED), maxScroll));
        return true;
    }

    // ───── ヘルパー ─────

    private void requestList() { waiting = true; ScoreboardPacketClient.requestPlayerList(); }

    private static String onOff(boolean v) { return v ? "§aON" : "§cOFF"; }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

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
            return (cfg.scoreboardScale - 0.5f) / 2.5f;
        }

        @Override
        public void setValueRelative(double rel) {
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