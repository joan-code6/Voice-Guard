package de.joancode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PrivacyCommand implements CommandExecutor {
    private final VoiceGuard plugin;
    private final PrivacyManager privacyManager;

    public PrivacyCommand(VoiceGuard plugin, PrivacyManager privacyManager) {
        this.plugin = plugin;
        this.privacyManager = privacyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("Usage: /privacy <accept|deny|opt-out>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept":
                privacyManager.setConsent(player.getUniqueId(), true);
                // remove blindness and show a thank-you title
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                String okHeader = plugin.getConfig().getString("voiceguard.privacy.title.header", "Thank you");
                String okSubtitle = plugin.getConfig().getString("voiceguard.privacy.title.accept_subtitle", "You accepted the terms.");
                int fadeInOk = plugin.getConfig().getInt("voiceguard.privacy.title.fade_in", 10);
                int stayOk = plugin.getConfig().getInt("voiceguard.privacy.title.stay", 70);
                int fadeOutOk = plugin.getConfig().getInt("voiceguard.privacy.title.fade_out", 20);
                player.sendTitle(okHeader, okSubtitle, fadeInOk, stayOk, fadeOutOk);
                plugin.getLogger().info(player.getName() + " accepted privacy consent.");
                break;
            case "deny":
                privacyManager.setConsent(player.getUniqueId(), false);
                player.kickPlayer(plugin.getConfig().getString("voiceguard.privacy.kick_message", "You must accept voice monitoring to play on this server."));
                plugin.getLogger().info(player.getName() + " denied privacy consent and was kicked.");
                break;
            case "opt-out":
                privacyManager.setConsent(player.getUniqueId(), false);
                player.kickPlayer(plugin.getConfig().getString("voiceguard.privacy.kick_message", "You must accept voice monitoring to play on this server."));
                break;
            default:
                player.sendMessage("Usage: /privacy <accept|deny|opt-out>");
        }
        return true;
    }
}
