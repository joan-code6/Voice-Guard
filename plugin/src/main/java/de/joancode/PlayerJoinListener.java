package de.joancode;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
            // Apply blindness effect and show a title prompting the player to read TOS and accept/deny
            int durationTicks = plugin.getConfig().getInt("voiceguard.privacy.title.stay", 200);
            // Add some extra time so blindness lasts while title is showing
            int blindnessDuration = Math.max(200, durationTicks) + 40;
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 1));

            String header = plugin.getConfig().getString("voiceguard.privacy.title.header", "READ THE TOS");
            String subtitle = plugin.getConfig().getString("voiceguard.privacy.title.subtitle", "Read the terms and /privacy accept or /privacy deny");
            String tos = plugin.getConfig().getString("voiceguard.privacy.tos_url", "https://example.com/terms");
            subtitle = subtitle.replace("%tos_url%", tos);

            int fadeIn = plugin.getConfig().getInt("voiceguard.privacy.title.fade_in", 10);
            int stay = plugin.getConfig().getInt("voiceguard.privacy.title.stay", 200);
            int fadeOut = plugin.getConfig().getInt("voiceguard.privacy.title.fade_out", 20);
            player.sendTitle(header, subtitle, fadeIn, stay, fadeOut);
        }
    }
}
