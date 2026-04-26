package dev.tamago0314.hikariTweaks.scoreboard;

/**
 * サーバーから受信したプレイヤー一覧の 1 行分データ。
 */
public record PlayerListEntry(
        String uuid,
        String displayName,
        boolean isBot,
        boolean isBlocked
) {}
