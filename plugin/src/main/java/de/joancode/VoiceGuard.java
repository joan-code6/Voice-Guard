package de.joancode;

import org.bukkit.plugin.java.JavaPlugin;

public class VoiceGuard extends JavaPlugin {
    private BackendClient backendClient;

    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();
        // Initialize backend client
        String backendUrl = getConfig().getString("voiceguard.backend.url", "http://localhost:8000");
        int timeout = getConfig().getInt("voiceguard.backend.timeout", 30);
        backendClient = new BackendClient(this, backendUrl, timeout);
        // Register voice chat listener (Simple Voice Chat API)
        VoiceChatListener.register(this, backendClient);
        getLogger().info("VoiceGuard enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoiceGuard disabled.");
    }

    public BackendClient getBackendClient() {
        return backendClient;
    }
}
    // ...existing code...
