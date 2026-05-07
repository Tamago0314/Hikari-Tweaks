package com.hikariserver.hikaritweaks.scoreboard;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side packet utility for HikariScoreBoard integration.
 */
public final class ScoreboardPacketClient {

    private static final Identifier RANKING_DATA         = new Identifier("hikariscoreboard", "ranking_data");
    private static final Identifier PLAYER_LIST_REQUEST  = new Identifier("hikariscoreboard", "player_list_request");
    private static final Identifier PLAYER_LIST_RESPONSE = new Identifier("hikariscoreboard", "player_list_response");
    private static final Identifier BLOCK_TOGGLE         = new Identifier("hikariscoreboard", "block_toggle");
    /** Server → Client: バニラサイドバーの表示/非表示をクライアントに指示（Hikari-Tweaks連携） */
    private static final Identifier VANILLA_SIDEBAR_CONTROL = new Identifier("hikariscoreboard", "vanilla_sidebar_control");

    private static List<PlayerListEntry> cachedList = new ArrayList<>();
    private static volatile RankingData cachedRanking = null;
    /** サーバーから hide 指示を受けた状態。true の間はデータが届いても HUD を表示しない */
    private static volatile boolean serverHidden = false;
    private static Consumer<List<PlayerListEntry>> onListUpdated = null;
    private static Runnable onRankingUpdated = null;

    private ScoreboardPacketClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RANKING_DATA,
                (client, handler, buf, responseSender) -> {
                    boolean isHidden = buf.readBoolean();
                    if (isHidden) {
                        client.execute(() -> {
                            serverHidden = true;
                            cachedRanking = null;
                        });
                        return;
                    }

                    String title = buf.readString(256);
                    int count = buf.readVarInt();
                    List<RankingEntry> top = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        String name = buf.readString(64);
                        long value = buf.readLong();
                        top.add(new RankingEntry(name, value));
                    }

                    long serverTotal = buf.readLong();
                    int selfRank = buf.readVarInt();
                    long selfValue = buf.readLong();
                    String selfName = buf.readString(64);

                    List<RankingEntry> full;
                    if (buf.isReadable()) {
                        int fullCount = buf.readVarInt();
                        full = new ArrayList<>(fullCount);
                        for (int i = 0; i < fullCount; i++) {
                            String name = buf.readString(64);
                            long val = buf.readLong();
                            full.add(new RankingEntry(name, val));
                        }
                    } else {
                        full = top;
                    }

                    RankingData data = new RankingData(title, top, full, serverTotal, selfRank, selfValue, selfName);
                    client.execute(() -> {
                        serverHidden = false;
                        cachedRanking = data;
                        if (onRankingUpdated != null) {
                            onRankingUpdated.run();
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PLAYER_LIST_RESPONSE,
                (client, handler, buf, responseSender) -> {
                    int count = buf.readVarInt();
                    List<PlayerListEntry> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        String uuid = buf.readString(36);
                        String displayName = buf.readString(64);
                        boolean isBot = buf.readBoolean();
                        boolean isBlocked = buf.readBoolean();
                        list.add(new PlayerListEntry(uuid, displayName, isBot, isBlocked));
                    }
                    client.execute(() -> {
                        cachedList = list;
                        if (onListUpdated != null) {
                            onListUpdated.accept(list);
                        }
                    });
                });

        // HikariScoreBoard から「バニラサイドバーを非表示にする」指示を受信（Hikari-Tweaks 連携）
        // Hikari-Tweaks がない場合はこのパケットは届かないため副作用なし
        ClientPlayNetworking.registerGlobalReceiver(VANILLA_SIDEBAR_CONTROL,
                (client, handler, buf, responseSender) -> {
                    boolean hideVanilla = buf.readBoolean();
                    client.execute(() -> {
                        com.hikariserver.hikaritweaks.config.ClientConfigManager.config.scoreboardHideVanilla = hideVanilla;
                        com.hikariserver.hikaritweaks.config.ClientConfigManager.save();
                    });
                });
    }

    public static void requestPlayerList() {
        if (ClientPlayNetworking.canSend(PLAYER_LIST_REQUEST)) {
            ClientPlayNetworking.send(PLAYER_LIST_REQUEST, PacketByteBufs.empty());
        }
    }

    /**
     * 指定したプレイヤーのブロック状態を必要なら変更する。
     * 一括操作（まとめて表示/非表示）で使用する。
     *
     * @param uuid      対象プレイヤーの UUID 文字列
     * @param shouldBlock true = 非表示にしたい, false = 表示したい
     */
    public static void toggleBlockIfNeeded(String uuid, boolean shouldBlock) {
        // キャッシュからブロック状態を確認し、目的と異なる場合のみトグル
        boolean currentlyBlocked = cachedList.stream()
                .filter(e -> e.uuid().equals(uuid))
                .findFirst()
                .map(PlayerListEntry::isBlocked)
                .orElse(false);

        if (currentlyBlocked != shouldBlock) {
            toggleBlock(uuid);
        }
    }

    public static void toggleBlock(String uuid) {
        if (ClientPlayNetworking.canSend(BLOCK_TOGGLE)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(uuid, 36);
            ClientPlayNetworking.send(BLOCK_TOGGLE, buf);
        }
    }

    public static void setOnListUpdated(Consumer<List<PlayerListEntry>> callback) {
        onListUpdated = callback;
    }

    public static void setOnRankingUpdated(Runnable callback) {
        onRankingUpdated = callback;
    }

    public static List<PlayerListEntry> getCachedList() {
        return cachedList;
    }

    public static RankingData getCachedRanking() {
        return cachedRanking;
    }

    public static void clearRanking() {
        cachedRanking = null;
    }

    /** サーバーから非表示指示を受けている状態かどうか */
    public static boolean isServerHidden() {
        return serverHidden;
    }

    /** サーバー切断時などに状態をリセットする */
    public static void resetHiddenState() {
        serverHidden = false;
        cachedRanking = null;
    }

    public record RankingEntry(String name, long value) {}

    public record RankingData(
            String title,
            List<RankingEntry> top,
            List<RankingEntry> full,
            long serverTotal,
            int selfRank,
            long selfValue,
            String selfName
    ) {}
}
