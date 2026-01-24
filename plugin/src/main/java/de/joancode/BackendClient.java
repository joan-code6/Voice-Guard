package de.joancode;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BackendClient {
    private final OkHttpClient client;
    private final String backendUrl;
    private final HttpUrl analyzeUrl;
    private final Plugin plugin;
    private final long minIntervalMs;
    private final AtomicLong lastSentMs = new AtomicLong(0);

    public BackendClient(Plugin plugin, String backendUrl, int timeoutSeconds, long minIntervalMs) {
        this.plugin = plugin;
        this.backendUrl = normalizeBackendUrl(backendUrl);
        this.minIntervalMs = Math.max(0, minIntervalMs);
        this.client = new OkHttpClient.Builder()
                .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        HttpUrl base = HttpUrl.parse(this.backendUrl);
        if (base == null) {
            throw new IllegalArgumentException("Invalid backend URL: " + backendUrl);
        }
        this.analyzeUrl = base.newBuilder().addPathSegment("analyze").build();
        this.plugin.getLogger().info("Using backend URL: " + this.analyzeUrl);
    }

    private String normalizeBackendUrl(String url) {
        if (url == null) return "http://localhost:8000";
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }
        // remove trailing slash
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public void sendAudio(File audioFile, String playerUuid, String playerName, String timestamp, String serverId) {
        long now = System.currentTimeMillis();
        long prev = lastSentMs.get();
        if (now - prev < minIntervalMs) {
            long skipMs = minIntervalMs - (now - prev);
            plugin.getLogger().fine("Throttling mic packet, skipping ~" + skipMs + "ms early");
            if (audioFile != null) {
                // cleanup skipped temp file
                //noinspection ResultOfMethodCallIgnored
                audioFile.delete();
            }
            return;
        }
        lastSentMs.set(now);
        plugin.getLogger().info("Sending audio to backend for player " + playerName + " (" + playerUuid + ") at " + timestamp);
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("audio", audioFile != null ? audioFile.getName() : "null", audioFile != null ? RequestBody.create(audioFile, MediaType.parse("audio/opus")) : null)
                .addFormDataPart("player_uuid", playerUuid)
                .addFormDataPart("player_name", playerName)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("server_id", serverId)
                .build();
        Request request = new Request.Builder()
                .url(analyzeUrl)
                .post(requestBody)
                .build();
        plugin.getLogger().info("Sending audio file to backend: " + analyzeUrl);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Bukkit.getLogger().warning("[VoiceGuard] Backend request failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (audioFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    audioFile.delete();
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Bukkit.getLogger().warning("[VoiceGuard] Backend returned error: " + response.code());
                } else {

                }
                response.close();
                if (audioFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    audioFile.delete();
                }
            }
        });
    }
}
