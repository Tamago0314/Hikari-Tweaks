package dev.tamago0314.hikariTweaks.gui;

import dev.tamago0314.hikariTweaks.config.ClientConfig;
import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardPacketClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * スコアボードの位置をドラッグ&ドロップで調整できる専用画面。
 *
 * - スコアボードのプレビューを画面上に表示しドラッグで移動できる
 * - スケールスライダーで拡大縮小を調整できる
 * - 「確定」で設定を保存、「キャンセル」で破棄
 */
public final class PositionEditorScreen extends Screen {

    private static final int PREVIEW_W = 130;
    private static final int LINE_H    = 9;

    private final Screen parent;

    // 確定前の一時値
    private int   tempX;     // 0-100%
    private int   tempY;     // 0-100%
    private float tempScale; // 0.5-3.0

    // ドラッグ状態
    private boolean dragging    = false;
    private int     dragOffsetX = 0;
    private int     dragOffsetY = 0;

    private ScaleSlider scaleSlider;

    public PositionEditorScreen(Screen parent) {
        super(new LiteralText("スコアボード位置調整"));
        this.parent    = parent;
        ClientConfig cfg = ClientConfigManager.config;
        this.tempX     = cfg.scoreboardPositionX;
        this.tempY     = cfg.scoreboardPositionY;
        this.tempScale = cfg.scoreboardScale;
    }

    @Override
    protected void init() {
        super.init();

        // スケールスライダー
        scaleSlider = new ScaleSlider(
                width / 2 - 100, height - 68,
                200, 20,
                (tempScale - 0.5f) / 2.5f // 0.5-3.0 → 0-1
        );
        addDrawableChild(scaleSlider);

        // 確定ボタン
        addDrawableChild(new ButtonWidget(
                width / 2 - 105, height - 42, 100, 20,
                new LiteralText("§a確定"),
                btn -> {
                    ClientConfig cfg = ClientConfigManager.config;
                    cfg.scoreboardPositionX = tempX;
                    cfg.scoreboardPositionY = tempY;
                    cfg.scoreboardScale     = tempScale;
                    ClientConfigManager.save();
                    close();
                }
        ));

        // キャンセルボタン
        addDrawableChild(new ButtonWidget(
                width / 2 + 5, height - 42, 100, 20,
                new LiteralText("§cキャンセル"),
                btn -> close()
        ));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // 説明文
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String msg1 = "§eスコアボードをドラッグして位置を調整できます";
        tr.drawWithShadow(matrices, msg1, (width - tr.getWidth(msg1)) / 2f, 12, 0xFFFFFF);
        String msg2 = "§7スケール: " + String.format("%.2f", tempScale) + "x";
        tr.drawWithShadow(matrices, msg2, width / 2f + 110, height - 63, 0xFFFFFF);

        renderPreview(matrices, mouseX, mouseY);

        super.render(matrices, mouseX, mouseY, delta);
    }

    // ── プレビュー描画 ─────────────────────────────────────

    private void renderPreview(MatrixStack matrices, int mouseX, int mouseY) {
        // プレビュー用ダミーデータ
        String title = "スコアボード";
        String[] names  = {"Player1", "Player2", "Player3", "Player4", "Player5"};
        int[]    scores = {1234, 987, 756, 543, 210};

        int lines   = names.length;
        int lineH_  = LINE_H;
        int boxW    = PREVIEW_W;
        int boxH    = (lines + 1) * lineH_ + 2;

        float sc = tempScale;
        int scaledW = (int)(boxW * sc);
        int scaledH = (int)(boxH * sc);

        // % → ピクセル変換
        int anchorX = (int)(width  * tempX / 100.0);
        int anchorY = (int)(height * tempY / 100.0);

        int drawX = anchorX - scaledW;
        int drawY = anchorY - scaledH / 2;

        // スケール適用
        matrices.push();
        matrices.translate(anchorX, anchorY, 0);
        matrices.scale(sc, sc, 1.0f);
        matrices.translate(-anchorX / sc, -anchorY / sc, 0);

        int lX = (int)(anchorX / sc) - boxW;
        int lY = (int)(anchorY / sc) - boxH / 2;

        ClientConfig cfg = ClientConfigManager.config;

        // ヘッダー
        DrawableHelper.fill(matrices, lX, lY, lX + boxW, lY + lineH_ + 1,
                cfg.scoreboardHeaderColor);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int titleW = tr.getWidth(title);
        tr.drawWithShadow(matrices, title, lX + (boxW - titleW) / 2, lY + 1,
                cfg.scoreboardTextColor);

        // エントリ
        for (int i = 0; i < lines; i++) {
            int rowY = lY + (i + 1) * lineH_ + 1;
            DrawableHelper.fill(matrices, lX, rowY, lX + boxW, rowY + lineH_,
                    cfg.scoreboardBodyColor);

            String rank   = String.format("%2d ", i + 1);
            int rankW     = tr.getWidth(rank);
            tr.drawWithShadow(matrices, rank,     lX + 2,                   rowY, 0xFFAAAAAA);
            tr.drawWithShadow(matrices, names[i], lX + 2 + rankW,           rowY, cfg.scoreboardTextColor);
            String sc_str = String.valueOf(scores[i]);
            tr.drawWithShadow(matrices, sc_str, lX + boxW - tr.getWidth(sc_str) - 2,
                    rowY, cfg.scoreboardScoreColor);
        }

        // ホバー or ドラッグ中は黄色ボーダー
        boolean hovered = mouseX >= drawX && mouseX <= drawX + scaledW
                && mouseY >= drawY && mouseY <= drawY + scaledH;
        if (hovered || dragging) {
            int bx = lX - 1;
            int by = lY - 1;
            int bxe = lX + boxW + 1;
            int bye = lY + boxH + 1;
            int c = 0xFFFFFF00;
            DrawableHelper.fill(matrices, bx,  by,  bxe, by + 1, c);
            DrawableHelper.fill(matrices, bx,  bye - 1, bxe, bye, c);
            DrawableHelper.fill(matrices, bx,  by,  bx + 1, bye, c);
            DrawableHelper.fill(matrices, bxe - 1, by,  bxe, bye, c);
        }

        matrices.pop();
    }

    // ── 入力処理 ─────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverPreview((int)mouseX, (int)mouseY)) {
            dragging = true;
            float sc  = tempScale;
            int scaledW = (int)(PREVIEW_W * sc);
            int scaledH = (int)(calcPreviewH() * sc);
            int drawX   = (int)(width  * tempX / 100.0) - scaledW;
            int drawY   = (int)(height * tempY / 100.0) - scaledH / 2;
            dragOffsetX = (int)mouseX - drawX;
            dragOffsetY = (int)mouseY - drawY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (dragging && button == 0) {
            float sc    = tempScale;
            int scaledW = (int)(PREVIEW_W * sc);
            int scaledH = (int)(calcPreviewH() * sc);
            int newAnchorX = (int)mouseX - dragOffsetX + scaledW;
            int newAnchorY = (int)mouseY - dragOffsetY + scaledH / 2;
            tempX = Math.max(0, Math.min(100, (int)(newAnchorX * 100.0 / width)));
            tempY = Math.max(0, Math.min(100, (int)(newAnchorY * 100.0 / height)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── ヘルパー ─────────────────────────────────────────────

    private boolean isMouseOverPreview(int mouseX, int mouseY) {
        float sc    = tempScale;
        int scaledW = (int)(PREVIEW_W * sc);
        int scaledH = (int)(calcPreviewH() * sc);
        int drawX   = (int)(width  * tempX / 100.0) - scaledW;
        int drawY   = (int)(height * tempY / 100.0) - scaledH / 2;
        return mouseX >= drawX && mouseX <= drawX + scaledW
                && mouseY >= drawY && mouseY <= drawY + scaledH;
    }

    private static int calcPreviewH() {
        return (5 + 1) * LINE_H + 2;
    }

    // ── スケールスライダー ────────────────────────────────────

    private class ScaleSlider extends SliderWidget {
        ScaleSlider(int x, int y, int w, int h, double initValue) {
            super(x, y, w, h, new LiteralText(""), initValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText(
                    "スケール: " + String.format("%.2f", tempScale) + "x"));
        }

        @Override
        protected void applyValue() {
            tempScale = (float)(0.5 + value * 2.5); // 0-1 → 0.5-3.0
            updateMessage();
        }
    }
}