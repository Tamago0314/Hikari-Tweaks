package dev.tamago0314.hikariTweaks.gui;

import dev.tamago0314.hikariTweaks.config.ClientConfig;
import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * スコアボードの色を個別に設定できる画面。
 *
 * 設定項目:
 *   - ヘッダー背景色 (ARGB)
 *   - 本文背景色    (ARGB)
 *   - テキスト色    (ARGB)
 *   - スコア色      (ARGB)
 *   - 自分強調色    (ARGB)
 *
 * 各色は A/R/G/B 4スライダーで調整し、プレビューをリアルタイムで確認できる。
 */
public final class ColorPickerScreen extends Screen {

    // 編集対象
    public enum Target {
        HEADER ("ヘッダー背景"),
        BODY   ("本文背景"),
        TEXT   ("テキスト"),
        SCORE  ("スコア数値"),
        SELF   ("自分強調");

        public final String label;
        Target(String label) { this.label = label; }
    }

    private static final int SLIDER_W = 200;
    private static final int SLIDER_H = 16;
    private static final int PREVIEW_SIZE = 40;

    private final Screen parent;
    private Target activeTarget = Target.HEADER;

    // 現在の各色（未保存）
    private int[] colors; // INDEX: Header, Body, Text, Score, Self

    private ComponentSlider sliderA, sliderR, sliderG, sliderB;

    public ColorPickerScreen(Screen parent) {
        super(new LiteralText("スコアボード色設定"));
        this.parent = parent;
        ClientConfig cfg = ClientConfigManager.config;
        this.colors = new int[]{
                cfg.scoreboardHeaderColor,
                cfg.scoreboardBodyColor,
                cfg.scoreboardTextColor,
                cfg.scoreboardScoreColor,
                cfg.scoreboardSelfColor
        };
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int cx = width / 2;

        // ターゲット選択ボタン
        Target[] targets = Target.values();
        int tabW = (SLIDER_W + 4) / targets.length;
        for (int i = 0; i < targets.length; i++) {
            final Target t = targets[i];
            int bx = cx - (SLIDER_W + 4) / 2 + i * tabW;
            boolean active = (t == activeTarget);
            addDrawableChild(new ButtonWidget(
                    bx, 30, tabW - 2, 14,
                    new LiteralText(active ? "§e" + t.label : t.label),
                    btn -> { activeTarget = t; rebuildWidgets(); }
            ));
        }

        int currentColor = colors[activeTarget.ordinal()];
        int startY = 58;
        int step   = SLIDER_H + 6;

        // A/R/G/B スライダー
        sliderA = new ComponentSlider(cx - SLIDER_W / 2, startY,          SLIDER_W, SLIDER_H, "A", (currentColor >> 24) & 0xFF);
        sliderR = new ComponentSlider(cx - SLIDER_W / 2, startY + step,   SLIDER_W, SLIDER_H, "R", (currentColor >> 16) & 0xFF);
        sliderG = new ComponentSlider(cx - SLIDER_W / 2, startY + step*2, SLIDER_W, SLIDER_H, "G", (currentColor >> 8)  & 0xFF);
        sliderB = new ComponentSlider(cx - SLIDER_W / 2, startY + step*3, SLIDER_W, SLIDER_H, "B",  currentColor        & 0xFF);
        addDrawableChild(sliderA);
        addDrawableChild(sliderR);
        addDrawableChild(sliderG);
        addDrawableChild(sliderB);

        // --- プリセット色ボタン ---
        String[] presetLabels  = {"白", "黒", "半透明黒", "赤", "緑", "黄", "水色"};
        int[]    presetColors  = {0xFFFFFFFF, 0xFF000000, 0x66000000, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFF55FFFF};
        int pw = 24, ph = 14, gap = 4;
        int totalW = presetLabels.length * (pw + gap) - gap;
        int px0 = cx - totalW / 2;
        int py  = startY + step * 4 + 4;
        for (int i = 0; i < presetLabels.length; i++) {
            final int color = presetColors[i];
            addDrawableChild(new ButtonWidget(
                    px0 + i * (pw + gap), py, pw, ph,
                    new LiteralText(presetLabels[i]),
                    btn -> { applyColor(color); rebuildWidgets(); }
            ));
        }

        // 保存ボタン
        addDrawableChild(new ButtonWidget(
                cx - 105, height - 28, 100, 20,
                new LiteralText("§a保存して戻る"),
                btn -> {
                    saveAll();
                    if (client != null) client.setScreen(parent);
                }
        ));
        // キャンセルボタン
        addDrawableChild(new ButtonWidget(
                cx + 5, height - 28, 100, 20,
                new LiteralText("§cキャンセル"),
                btn -> { if (client != null) client.setScreen(parent); }
        ));
    }

    private void applyColor(int color) {
        colors[activeTarget.ordinal()] = color;
    }

    private int buildColor() {
        int a = sliderA != null ? sliderA.getComponentValue() : 0xFF;
        int r = sliderR != null ? sliderR.getComponentValue() : 0xFF;
        int g = sliderG != null ? sliderG.getComponentValue() : 0xFF;
        int b = sliderB != null ? sliderB.getComponentValue() : 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void saveAll() {
        colors[activeTarget.ordinal()] = buildColor();
        ClientConfig cfg = ClientConfigManager.config;
        cfg.scoreboardHeaderColor = colors[Target.HEADER.ordinal()];
        cfg.scoreboardBodyColor   = colors[Target.BODY.ordinal()];
        cfg.scoreboardTextColor   = colors[Target.TEXT.ordinal()];
        cfg.scoreboardScoreColor  = colors[Target.SCORE.ordinal()];
        cfg.scoreboardSelfColor   = colors[Target.SELF.ordinal()];
        ClientConfigManager.save();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // タイトル
        TextRenderer tr2 = MinecraftClient.getInstance().textRenderer;
        String titleStr = "§e色設定 — " + activeTarget.label;
        tr2.drawWithShadow(matrices, titleStr, (width - tr2.getWidth(titleStr)) / 2f, 14, 0xFFFFFF);

        // カラープレビューパネル
        int previewX = width / 2 + SLIDER_W / 2 + 12;
        int previewY = 58;
        DrawableHelper.fill(matrices, previewX, previewY,
                previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE,
                colors[activeTarget.ordinal()]);
        int liveColor = buildColor();
        DrawableHelper.fill(matrices, previewX, previewY + PREVIEW_SIZE + 2,
                previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE * 2 + 2, liveColor);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        tr.drawWithShadow(matrices, "保存済", previewX, previewY + PREVIEW_SIZE + PREVIEW_SIZE + 6, 0xAAAAAA);
        tr.drawWithShadow(matrices, "現在値", previewX, previewY + PREVIEW_SIZE * 2 + 10, 0xFFFFFF);

        tr.drawWithShadow(matrices,
                String.format("#%08X", liveColor),
                width / 2 - SLIDER_W / 2, 58 + (SLIDER_H + 6) * 4 + 22, 0xCCCCCC);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private static class ComponentSlider extends SliderWidget {
        private final String label;
        private int componentValue;

        ComponentSlider(int x, int y, int w, int h, String label, int initialValue) {
            super(x, y, w, h, new LiteralText(""), initialValue / 255.0);
            this.label          = label;
            this.componentValue = initialValue;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText(label + ": " + componentValue));
        }

        @Override
        protected void applyValue() {
            componentValue = (int)Math.round(value * 255);
            updateMessage();
        }

        public int getComponentValue() { return componentValue; }
    }
}