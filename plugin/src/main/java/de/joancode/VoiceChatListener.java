package de.joancode;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VoiceChatListener implements VoicechatPlugin {

        @Override
        public String getPluginId() {
            return "voiceguard";
        }

    private static BackendClient backendClientRef;

    public static void register(Plugin plugin, BackendClient backendClient) {
        // Register with Simple Voice Chat API
        Bukkit.getLogger().info("[VoiceGuard] Registering VoiceChatListener (stub, implement with API)");
        backendClientRef = backendClient;
        // ...actual registration code with Simple Voice Chat API goes here...
    }

    // Example event handler (pseudo-code, replace with actual API usage)
    public void onMicrophonePacket(MicrophonePacketEvent event) {
        UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
        byte[] opusData = event.getPacket().getOpusEncodedData();
        long timestamp = System.currentTimeMillis();
        
        // Create a temporary file for the audio chunk
        File tempOpusFile = null;
        try {
            tempOpusFile = File.createTempFile("voiceguard_" + playerUuid + "_" + timestamp, ".opus");
            try (FileOutputStream fos = new FileOutputStream(tempOpusFile)) {
                fos.write(opusData);
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[VoiceGuard] Failed to create temp file for audio: " + e.getMessage());
            return;
        }
        
        // Send to backend
        if (backendClientRef != null) {
            backendClientRef.sendAudio(tempOpusFile, playerUuid.toString(), "PlayerName", String.valueOf(timestamp), "main-server");
            Bukkit.getLogger().info("[VoiceGuard] Sent audio chunk to backend for " + playerUuid);
        }
        
        // Clean up temp file after sending (optional, as it will be deleted on JVM exit)
        tempOpusFile.delete();
    }
}
