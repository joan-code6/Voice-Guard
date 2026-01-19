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
            getLogger().warning("'privacy' command not found in plugin resources, attempting dynamic registration...");
            try {
                // Create a PluginCommand instance via reflection
                java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> c = org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
                c.setAccessible(true);
                org.bukkit.command.PluginCommand dyn = c.newInstance("privacy", this);
                dyn.setExecutor(new PrivacyCommand(this, privacyManager));

                // Get the server's command map reflectively and register the command
                Object serverImpl = getServer();
                java.lang.reflect.Method getCommandMap = serverImpl.getClass().getMethod("getCommandMap");
                Object commandMap = getCommandMap.invoke(serverImpl);
                java.lang.reflect.Method register = commandMap.getClass().getMethod("register", String.class, org.bukkit.command.Command.class);
                register.invoke(commandMap, getDescription().getName(), dyn);
                getLogger().info("Dynamically registered 'privacy' command.");
            } catch (Exception e) {
                getLogger().warning("Failed to dynamically register 'privacy' command: " + e.getMessage());
            }
        }
        
        // accept/deny are handled as subcommands of /privacy now (PrivacyCommand)
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
