package de.joancode;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    private final VoiceGuard plugin;
    private final PrivacyManager privacyManager;

    public PlayerJoinListener(VoiceGuard plugin, PrivacyManager privacyManager) {
        this.plugin = plugin;
        this.privacyManager = privacyManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!privacyManager.hasConsented(player.getUniqueId())) {
            // Show consent popup (chat message)
            player.sendMessage(plugin.getConfig().getString("voiceguard.privacy.consent_message",
                "[VoiceGuard] This server monitors voice chat for safety. Type /accept to continue or /deny to leave."));
            player.sendMessage("Type /accept to continue or /deny to leave.");
        }
    }
}
