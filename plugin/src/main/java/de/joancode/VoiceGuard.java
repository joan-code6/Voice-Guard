
package de.joancode;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Constructor;

public class VoiceGuard extends JavaPlugin {
    private BackendClient backendClient;
    private PrivacyManager privacyManager;
    private boolean isFolia;

    @Override
    public void onEnable() {
        // Check if server is Folia
        isFolia = getServer().getName().equalsIgnoreCase("Folia");
        if (isFolia) {
            getLogger().info("Detected Folia server. Ensuring Folia compatibility.");
        }
        // Load config
        saveDefaultConfig();
        // Initialize backend client
        String backendUrl = getConfig().getString("voiceguard.backend.url", "http://localhost:8000");
        int timeout = getConfig().getInt("voiceguard.backend.timeout", 30);
        long minIntervalMs = getConfig().getLong("voiceguard.backend.min_interval_ms", 200L);
        backendClient = new BackendClient(this, backendUrl, timeout, minIntervalMs);
        // Initialize privacy manager
        privacyManager = new PrivacyManager(this);
        // Set backend client for voice chat listener
        VoiceChatListener.setBackendClient(backendClient);

        // Register with Simple Voice Chat via Bukkit service
        try {
            BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
            if (service != null) {
                service.registerPlugin(new VoiceChatListener());
                getLogger().info("Registered VoiceGuard as a Simple Voice Chat plugin.");
            } else {
                getLogger().warning("Simple Voice Chat service not found. Is the 'voicechat' plugin enabled?");
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to register with Simple Voice Chat: " + t.getMessage());
        }

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, privacyManager), this);

        // Register commands dynamically
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand privacyCommand = constructor.newInstance("privacy", this);
            privacyCommand.setExecutor(new PrivacyCommand(this));
            privacyCommand.setTabCompleter(new PrivacyCommand(this));
            getServer().getCommandMap().register(getName(), privacyCommand);
        } catch (Exception e) {
            getLogger().severe("Failed to register privacy command: " + e.getMessage());
        }

        getLogger().info("VoiceGuard enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoiceGuard disabled.");
    }

    public BackendClient getBackendClient() {
        return backendClient;
    }

    public PrivacyManager getPrivacyManager() {
        return privacyManager;
    }

    public boolean isFolia() {
        return isFolia;
    }
}
