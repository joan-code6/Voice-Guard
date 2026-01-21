package de.joancode;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivacyManager {
    private final Map<UUID, Boolean> consents = new HashMap<>();
    private final JavaPlugin plugin;
    private final File privacyFile;
    private FileConfiguration privacyConfig;

    public PrivacyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.privacyFile = new File(plugin.getDataFolder(), "privacy.yml");
        loadConsents();
    }

    private void loadConsents() {
        if (!privacyFile.exists()) {
            plugin.saveResource("privacy.yml", false);
        }
        privacyConfig = YamlConfiguration.loadConfiguration(privacyFile);
        for (String key : privacyConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            boolean consent = privacyConfig.getBoolean(key);
            consents.put(uuid, consent);
        }
    }

    private void saveConsents() {
        for (Map.Entry<UUID, Boolean> entry : consents.entrySet()) {
            privacyConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            privacyConfig.save(privacyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save privacy consents: " + e.getMessage());
        }
    }

    public void setConsent(UUID playerId, boolean consent) {
        consents.put(playerId, consent);
        saveConsents();
    }

    public boolean hasConsent(UUID playerId) {
        return consents.getOrDefault(playerId, false);
    }

    public void revokeConsent(UUID playerId) {
        consents.remove(playerId);
        privacyConfig.set(playerId.toString(), null);
        saveConsents();
    }
}