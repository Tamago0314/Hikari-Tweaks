package dev.tamago0314.hikariTweaks.scoreboard;

import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
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

    private static List<PlayerListEntry> cachedList = new ArrayList<>();
    private static volatile RankingData cachedRanking = null;
    private static Consumer<List<PlayerListEntry>> onListUpdated = null;
    private static Runnable onRankingUpdated = null;

    private ScoreboardPacketClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RANKING_DATA,
                (client, handler, buf, responseSender) -> {
                    boolean isHidden = buf.readBoolean();
                    if (isHidden) {
                        cachedRanking = null;
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
    }

    public static void requestPlayerList() {
        if (ClientPlayNetworking.canSend(PLAYER_LIST_REQUEST)) {
            ClientPlayNetworking.send(PLAYER_LIST_REQUEST, PacketByteBufs.empty());
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
