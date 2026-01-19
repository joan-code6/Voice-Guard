package de.joancode;

import org.bukkit.plugin.java.JavaPlugin;

public class VoiceGuard extends JavaPlugin {
    private PrivacyManager privacyManager;
    private BackendClient backendClient;

    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();
        // Initialize privacy manager
        privacyManager = new PrivacyManager(this);
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, privacyManager), this);
        // Register commands with null-safety checks
        org.bukkit.command.PluginCommand privacyCmd = getCommand("privacy");
        if (privacyCmd != null) {
            privacyCmd.setExecutor(new PrivacyCommand(this, privacyManager));
        } else {
            getLogger().warning("Failed to register 'privacy' command - command not found in paper-plugin.yml");
        }
        
        org.bukkit.command.PluginCommand acceptdenyCmd = getCommand("acceptdeny");
        if (acceptdenyCmd != null) {
            acceptdenyCmd.setExecutor(new AcceptDenyCommand(this, privacyManager));
        } else {
            getLogger().warning("Failed to register 'acceptdeny' command - command not found in paper-plugin.yml");
        }
        // Register voice chat listener (Simple Voice Chat API)
        VoiceChatListener.register(this, backendClient);
        getLogger().info("VoiceGuard enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoiceGuard disabled.");
    }

    public PrivacyManager getPrivacyManager() {
        return privacyManager;
    }

    public BackendClient getBackendClient() {
        return backendClient;
    }
}
    // ...existing code...
