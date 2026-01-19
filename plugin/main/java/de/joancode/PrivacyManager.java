package de.joancode;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivacyManager {
    private final Plugin plugin;
    private final File privacyFile;
    private final YamlConfiguration privacyConfig;
    private final Map<UUID, Boolean> consentCache = new HashMap<>();

    public PrivacyManager(Plugin plugin) {
        this.plugin = plugin;
        this.privacyFile = new File(plugin.getDataFolder(), "privacy.yml");
        if (!privacyFile.exists()) {
            try {
                privacyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.privacyConfig = YamlConfiguration.loadConfiguration(privacyFile);
    }

    public boolean hasConsented(UUID uuid) {
        if (consentCache.containsKey(uuid)) {
            return consentCache.get(uuid);
        }
        boolean consented = privacyConfig.getBoolean("players." + uuid + ".consented", false);
        consentCache.put(uuid, consented);
        return consented;
    }

    public void setConsent(UUID uuid, boolean consented) {
        privacyConfig.set("players." + uuid + ".consented", consented);
        privacyConfig.set("players." + uuid + ".timestamp", java.time.Instant.now().toString());
        consentCache.put(uuid, consented);
        save();
        plugin.getLogger().info("Consent for " + uuid + ": " + consented);
    }

    public void save() {
        try {
            privacyConfig.save(privacyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
