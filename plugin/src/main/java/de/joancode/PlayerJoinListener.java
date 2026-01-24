package de.joancode;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final VoiceGuard plugin;
    private final PrivacyManager privacyManager;
    private final boolean isFolia;
    private final Set<UUID> bookOpen = new HashSet<>();

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
                bookOpen.add(player.getUniqueId());
                player.openBook(book);
            };

            if (isFolia) {
                player.getScheduler().run(plugin, scheduledTask -> task.run(), () -> {});
            } else {
                task.run();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (bookOpen.contains(uuid)) {
            bookOpen.remove(uuid);
            privacyManager.setConsent(uuid, true);
            event.getPlayer().sendMessage("You accepted the TOS by closing the book. To deny use /privacy deny (which will kick you).");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (bookOpen.contains(uuid)) {
            bookOpen.remove(uuid);
            privacyManager.setConsent(uuid, true);
            event.getPlayer().sendMessage("You accepted the TOS by closing the book. To deny use /privacy deny (which will kick you).");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (bookOpen.contains(uuid)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Please close the book to accept the TOS.");
        }
    }
}
