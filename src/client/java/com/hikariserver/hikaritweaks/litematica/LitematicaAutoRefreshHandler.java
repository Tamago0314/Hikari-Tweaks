package com.hikariserver.hikaritweaks.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import net.minecraft.client.MinecraftClient;

/**
 * Litematica のマテリアルリスト自動 Refresh ハンドラ。
 *
 * MixinSchematicWorldRefresher がレイヤー変更を検知した際に
 * scheduleRefresh() を呼び出す。
 * 次の tick で reCreateMaterialList() を実行することで、
 * レイヤー変更直後の正しい状態でリストを更新する。
 */
public final class LitematicaAutoRefreshHandler {

    private static boolean refreshScheduled = false;

    private LitematicaAutoRefreshHandler() {}

    /**
     * MixinSchematicWorldRefresher から呼ばれる。
     * 次の tick で Refresh を実行するようスケジュールする。
     */
    public static void scheduleRefresh() {
        refreshScheduled = true;
    }

    /**
     * ClientTickEvents.END_CLIENT_TICK から毎 tick 呼ばれる。
     * scheduleRefresh() でセットされたフラグがあれば Refresh を実行する。
     */
    public static void tick(MinecraftClient client) {
        if (!refreshScheduled) {
            return;
        }
        refreshScheduled = false;

        if (client.world == null || client.player == null) {
            return;
        }

        MaterialListBase materialList = DataManager.getMaterialList();
        if (materialList != null) {
            materialList.reCreateMaterialList();
        }
    }

    /** ワールド切り替え時にリセット */
    public static void reset() {
        refreshScheduled = false;
    }
}
