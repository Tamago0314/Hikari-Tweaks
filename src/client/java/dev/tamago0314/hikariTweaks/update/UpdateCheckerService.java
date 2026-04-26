package dev.tamago0314.hikariTweaks.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tamago0314.hikariTweaks.HikariTweaksClient;
import dev.tamago0314.hikariTweaks.config.ClientConfig;
import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * GitHub Releases を参照して更新を通知するサービス。
 */
public final class UpdateCheckerService {
    private static final Gson GSON = new Gson();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hikari-tweaks-update-checker");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean started = false;
    private static volatile boolean checking = false;
    private static volatile UpdateInfo latestUpdate = null;

    // JOIN 後に通知待ちのクライアントを保持する（チェック完了後に通知するため）
    private static volatile MinecraftClient pendingNotifyClient = null;

    private UpdateCheckerService() {}

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        EXECUTOR.scheduleWithFixedDelay(UpdateCheckerService::checkIfDueSafe, 8, 60, TimeUnit.SECONDS);
        EXECUTOR.execute(UpdateCheckerService::checkIfDueSafe);
    }

    /**
     * サーバーに JOIN したときに呼ぶ。
     * チェック済みなら即通知、未完了なら完了後に通知されるよう pending に登録してから
     * 強制チェックを走らせる。
     */
    public static void onJoin(MinecraftClient client) {
        ClientConfig config = ClientConfigManager.config;
        if (config == null || !config.updateCheckerEnabled || !config.updateNotifyOnJoin) {
            return;
        }
        if (latestUpdate != null) {
            // チェック済みなので即通知
            notifyPlayer(client, latestUpdate);
            return;
        }
        // チェック未完了 → pending 登録して強制チェック
        pendingNotifyClient = client;
        EXECUTOR.execute(UpdateCheckerService::checkNowSafe);
    }

    /** サーバーから DISCONNECT したときに呼ぶ。pending をクリアする。 */
    public static void onDisconnect() {
        pendingNotifyClient = null;
    }

    // ── 内部チェックロジック ─────────────────────────────────────────────────────

    private static void checkIfDueSafe() {
        try {
            checkIfDue(false);
        } catch (Exception ignored) {
            // ネットワーク由来の失敗は静かに無視
        }
    }

    private static void checkNowSafe() {
        try {
            checkIfDue(true);
        } catch (Exception ignored) {
        }
    }

    /**
     * @param force true のとき interval チェックを無視して即実行する
     */
    private static void checkIfDue(boolean force) {
        ClientConfig config = ClientConfigManager.config;
        if (config == null || !config.updateCheckerEnabled) {
            return;
        }
        long now = System.currentTimeMillis();
        long intervalMs = Math.max(5, config.updateCheckIntervalMinutes) * 60_000L;
        if (!force && (now - config.updateLastCheckedAt < intervalMs || checking)) {
            return;
        }
        if (checking) {
            return;
        }

        checking = true;
        try {
            ReleaseInfo release = null;
            try {
                release = fetchLatestRelease(config);
            } catch (IOException ignored) {
                // ネットワークエラー時は次回再試行
            }
            config.updateLastCheckedAt = now;
            if (release != null && VersionComparator.isNewer(release.version, HikariTweaksClient.getModVersion())) {
                latestUpdate = new UpdateInfo(release.version, release.name, release.url);
            }
            ClientConfigManager.save();

            // pending な JOIN クライアントがいれば通知
            MinecraftClient pending = pendingNotifyClient;
            if (pending != null && latestUpdate != null) {
                pendingNotifyClient = null;
                notifyPlayer(pending, latestUpdate);
            }
        } finally {
            checking = false;
        }
    }

    // ── GitHub API ────────────────────────────────────────────────────────────

    private static ReleaseInfo fetchLatestRelease(ClientConfig config) throws IOException {
        String owner = config.updateGithubOwner == null ? "" : config.updateGithubOwner.trim();
        String repo = config.updateGithubRepo == null ? "" : config.updateGithubRepo.trim();
        if (owner.isEmpty() || repo.isEmpty()) {
            return null;
        }

        if (!config.updateIncludePrerelease) {
            JsonObject obj = requestJsonObject(apiUrl(owner, repo, "/releases/latest"));
            if (obj == null || obj.has("draft") && obj.get("draft").getAsBoolean()) {
                return null;
            }
            return toReleaseInfo(obj, config);
        }

        JsonArray arr = requestJsonArray(apiUrl(owner, repo, "/releases?per_page=10"));
        if (arr == null) {
            return null;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("draft") && obj.get("draft").getAsBoolean()) {
                continue;
            }
            return toReleaseInfo(obj, config);
        }
        return null;
    }

    private static ReleaseInfo toReleaseInfo(JsonObject obj, ClientConfig config) {
        String tag = getString(obj, "tag_name");
        if (tag == null || tag.isBlank()) {
            return null;
        }
        String name = getString(obj, "name");
        if (name == null || name.isBlank()) {
            name = tag;
        }
        String url = getString(obj, "html_url");
        if (config.updateReleaseUrlOverride != null && !config.updateReleaseUrlOverride.isBlank()) {
            url = config.updateReleaseUrlOverride.trim();
        } else if (url == null || url.isBlank()) {
            url = "https://github.com/" + config.updateGithubOwner + "/" + config.updateGithubRepo + "/releases";
        }
        return new ReleaseInfo(tag, name, url);
    }

    private static JsonObject requestJsonObject(String url) throws IOException {
        String body = request(url);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonElement element = JsonParser.parseString(body);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray requestJsonArray(String url) throws IOException {
        String body = request(url);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonElement element = JsonParser.parseString(body);
        return element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String request(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(8_000);
        con.setReadTimeout(8_000);
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("User-Agent", "Hikari-Tweaks-UpdateChecker");
        con.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

        int code = con.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        if (stream == null) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (code >= 200 && code < 300) {
                return sb.toString();
            }
            return null;
        } finally {
            con.disconnect();
        }
    }

    private static String apiUrl(String owner, String repo, String path) {
        return "https://api.github.com/repos/" + owner + "/" + repo + path;
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    // ── 通知 ──────────────────────────────────────────────────────────────────

    private static void notifyPlayer(MinecraftClient client, UpdateInfo info) {
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            ClientConfig config = ClientConfigManager.config;
            if (config == null) {
                return;
            }
            if (info.version.equals(config.updateLastNotifiedVersion)) {
                return;
            }

            String current = HikariTweaksClient.getModVersion();

            // ── 1行目：アップデート通知 ──────────────────────────────
            MutableText prefix = new LiteralText("[" + HikariTweaksClient.MOD_NAME + "] ")
                    .setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            MutableText body = new LiteralText("Update available: " + info.version + " (current: " + current + ") ")
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
            MutableText link = new LiteralText("[Open Release]")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.GREEN)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, info.url)));

            client.player.sendMessage(prefix.copy().append(body).append(link), false);

            // ── 2行目：URLをそのまま表示（コピーしやすいようにクリック可能） ──
            MutableText urlLabel = new LiteralText("  → ")
                    .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            MutableText urlText = new LiteralText(info.url)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.GRAY)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, info.url)));

            client.player.sendMessage(urlLabel.copy().append(urlText), false);

            config.updateLastNotifiedVersion = info.version;
            ClientConfigManager.save();
        });
    }

    private record ReleaseInfo(String version, String name, String url) {}

    private record UpdateInfo(String version, String name, String url) {}
}