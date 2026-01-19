package de.joancode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (args.length > 0 && args[0].equalsIgnoreCase("opt-out")) {
            privacyManager.setConsent(player.getUniqueId(), false);
            player.kickPlayer(plugin.getConfig().getString("voiceguard.privacy.kick_message", "You must accept voice monitoring to play on this server."));
            return true;
        }
        player.sendMessage("Usage: /privacy opt-out");
        return true;
    }
}
