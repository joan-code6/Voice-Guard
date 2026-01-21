package de.joancode;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class VoiceChatListener implements VoicechatPlugin {

    public VoiceChatListener() {
        Bukkit.getLogger().info("[VoiceGuard] VoiceChatListener instantiated");
    }

    @Override
    public String getPluginId() {
        return "voiceguard";
    }

    @Override
    public void initialize(VoicechatApi api) {
        Bukkit.getLogger().info("[VoiceGuard] VoiceChatListener initialized with API");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private static BackendClient backendClient;

    public static void setBackendClient(BackendClient client) {
        backendClient = client;
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        try {
            UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
            byte[] opusData = event.getPacket().getOpusEncodedData();
            long timestamp = System.currentTimeMillis();
            Bukkit.getLogger().info("[VoiceGuard] Microphone packet received for " + playerUuid);

            File tempOpusFile = File.createTempFile("voiceguard_" + playerUuid + "_" + timestamp, ".opus");
            try (FileOutputStream fos = new FileOutputStream(tempOpusFile)) {
                fos.write(opusData);
            }

            if (backendClient != null) {
                backendClient.sendAudio(tempOpusFile, playerUuid.toString(), playerUuid.toString(), String.valueOf(timestamp), "main-server");
                Bukkit.getLogger().info("[VoiceGuard] Queued audio chunk for backend (async) for " + playerUuid);
            } else {
                Bukkit.getLogger().warning("[VoiceGuard] Backend client is null, cannot send audio");
            }

            // Cleanup now handled by BackendClient after response/failure
        } catch (IOException e) {
            Bukkit.getLogger().warning("[VoiceGuard] Failed to process microphone packet: " + e.getMessage());
        }
    }
}
