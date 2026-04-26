package dev.tamago0314.hikariTweaks.mixin;

import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import dev.tamago0314.hikariTweaks.scoreboard.ScoreboardHudRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * バニラのサイドバー描画をインターセプトし、
 * カスタム HUD 描画に差し替える。
 *
 * scoreboardCustomHud が false の場合はバニラのままにする。
 */
@Mixin(InGameHud.class)
public class MixinInGameHud {

    /** バニラのサイドバー描画を差し替える */
    @Inject(
        method = "renderScoreboardSidebar",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hikariTweaks$replaceScoreboardSidebar(
            MatrixStack matrices,
            ScoreboardObjective objective,
            CallbackInfo ci) {
        if (!ClientConfigManager.config.scoreboardCustomHud) return;
        if (!ClientConfigManager.config.scoreboardHideVanilla) return;

        // バニラ描画をキャンセル（カスタム描画は renderHud で行う）
        ci.cancel();
    }

    /** renderHud の末尾でカスタム HUD を描画する */
    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void hikariTweaks$renderCustomHud(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ScoreboardHudRenderer.render(matrices);
    }
}
