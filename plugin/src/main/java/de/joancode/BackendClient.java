package de.joancode;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BackendClient {
    private final OkHttpClient client;
    private final String backendUrl;
    private final Plugin plugin;

    public BackendClient(Plugin plugin, String backendUrl, int timeoutSeconds) {
        this.plugin = plugin;
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient.Builder()
                .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    public void sendAudio(File audioFile, String playerUuid, String playerName, String timestamp, String serverId) {
        plugin.getLogger().info("Sending audio to backend for player " + playerName + " (" + playerUuid + ") at " + timestamp);
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("audio", audioFile != null ? audioFile.getName() : "null", audioFile != null ? RequestBody.create(audioFile, MediaType.parse("audio/opus")) : null)
                .addFormDataPart("player_uuid", playerUuid)
                .addFormDataPart("player_name", playerName)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("server_id", serverId)
                .build();
        Request request = new Request.Builder()
                .url(backendUrl + "/analyze")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Bukkit.getLogger().warning("[VoiceGuard] Backend request failed: " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Bukkit.getLogger().warning("[VoiceGuard] Backend returned error: " + response.code());
                } else {
                    Bukkit.getLogger().info("[VoiceGuard] Backend response: " + response.body().string());
                }
                response.close();
            }
        });
    }
}
