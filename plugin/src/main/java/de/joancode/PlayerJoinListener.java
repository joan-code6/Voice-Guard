package de.joancode;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;

public class PlayerJoinListener implements Listener {
    private final VoiceGuard plugin;
    private final PrivacyManager privacyManager;
    private final boolean isFolia;

    public PlayerJoinListener(VoiceGuard plugin, PrivacyManager privacyManager) {
        this.plugin = plugin;
        this.privacyManager = privacyManager;
        this.isFolia = plugin.isFolia();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!privacyManager.hasConsent(player.getUniqueId())) {
            Runnable task = () -> {
                // Set to spectator mode to prevent interaction
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                // Apply blindness effect
                int durationTicks = plugin.getConfig().getInt("voiceguard.privacy.title.stay", 200);
                // Add some extra time so blindness lasts while title is showing
                int blindnessDuration = Math.max(200, durationTicks) + 40;
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 1));

                // Create and open the TOS book
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta meta = (BookMeta) book.getItemMeta();
                if (meta != null) {
                    meta.setTitle("Terms of Service");
                    meta.setAuthor("VoiceGuard");
                    String tosText = plugin.getConfig().getString("voiceguard.privacy.tos_text", "Terms of Service not configured.");
                    // Split the text into pages if necessary
                    String[] pages = tosText.split("(?<=\\G.{250})"); // Split every 250 characters
                    meta.setPages(pages);
                    book.setItemMeta(meta);
                }
                player.openBook(book);
            };

            if (isFolia) {
                player.getScheduler().run(plugin, scheduledTask -> task.run(), () -> {});
            } else {
                task.run();
            }
        }
    }
}
