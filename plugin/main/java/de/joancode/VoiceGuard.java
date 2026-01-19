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
            // Attempt dynamic registration
            try {
                java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> ctor = org.bukkit.command.PluginCommand.class
                        .getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
                ctor.setAccessible(true);
                org.bukkit.command.PluginCommand dyn = ctor.newInstance("privacy", this);
                dyn.setDescription("Privacy consent and opt-out command");
                dyn.setUsage("/privacy <accept|deny|opt-out>");
                dyn.setExecutor(new PrivacyCommand(this, privacyManager));

                Object serverImpl = getServer();
                java.lang.reflect.Method getCommandMap = serverImpl.getClass().getMethod("getCommandMap");
                Object commandMap = getCommandMap.invoke(serverImpl);
                java.lang.reflect.Method register = commandMap.getClass().getMethod("register", String.class,
                        org.bukkit.command.Command.class);
                register.invoke(commandMap, getDescription().getName(), dyn);
                getLogger().info("Dynamically registered 'privacy' command.");
            } catch (Exception e) {
                getLogger().warning("Dynamic registration of 'privacy' failed: " + e.getMessage());
            }
        }
        
        org.bukkit.command.PluginCommand acceptdenyCmd = getCommand("acceptdeny");
        if (acceptdenyCmd != null) {
            acceptdenyCmd.setExecutor(new AcceptDenyCommand(this, privacyManager));
        } else {
            getLogger().warning("Failed to register 'acceptdeny' command - command not found in paper-plugin.yml");
            // Attempt dynamic registration
            try {
                java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> ctor = org.bukkit.command.PluginCommand.class
                        .getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
                ctor.setAccessible(true);
                org.bukkit.command.PluginCommand dyn = ctor.newInstance("acceptdeny", this);
                dyn.setDescription("Accept or deny privacy consent");
                dyn.setUsage("/acceptdeny <accept|deny>");
                dyn.setExecutor(new AcceptDenyCommand(this, privacyManager));

                Object serverImpl = getServer();
                java.lang.reflect.Method getCommandMap = serverImpl.getClass().getMethod("getCommandMap");
                Object commandMap = getCommandMap.invoke(serverImpl);
                java.lang.reflect.Method register = commandMap.getClass().getMethod("register", String.class,
                        org.bukkit.command.Command.class);
                register.invoke(commandMap, getDescription().getName(), dyn);
                getLogger().info("Dynamically registered 'acceptdeny' command.");
            } catch (Exception e) {
                getLogger().warning("Dynamic registration of 'acceptdeny' failed: " + e.getMessage());
            }
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
