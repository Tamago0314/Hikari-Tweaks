package com.hikariserver.hikaritweaks.mixin;

import com.hikariserver.hikaritweaks.config.TweaksOptions;
import com.hikariserver.hikaritweaks.litematica.LitematicaAutoRefreshHandler;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Litematica のレイヤー変更（updateBetweenY/X/Z）を検知し、
 * マテリアルリストを自動で Refresh する。
 *
 * Tweakermore の lmAutoRefreshMaterialList と同じアプローチ。
 * Litematica が存在しない環境では @Mixin の required=false により
 * このクラスは静かに無視される。
 */
@Mixin(value = SchematicWorldRefresher.class, remap = false)
public abstract class MixinSchematicWorldRefresher {

    @Inject(
            method = {"updateBetweenX", "updateBetweenY", "updateBetweenZ"},
            at = @At("HEAD"),
            remap = false
    )
    private void hikariTweaks$autoRefreshMaterialList(int min, int max, CallbackInfo ci) {
        if (TweaksOptions.AUTO_LITEMATICA_REFRESH.getBooleanValue()) {
            LitematicaAutoRefreshHandler.scheduleRefresh();
        }
    }
}
