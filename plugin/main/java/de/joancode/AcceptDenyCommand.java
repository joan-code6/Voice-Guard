package de.joancode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptDenyCommand implements CommandExecutor {
    private final VoiceGuard plugin;
    private final PrivacyManager privacyManager;

    public AcceptDenyCommand(VoiceGuard plugin, PrivacyManager privacyManager) {
        this.plugin = plugin;
        this.privacyManager = privacyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /acceptdeny <accept|deny>");
            return true;
        }
        Player player = (Player) sender;
        String subCmd = args[0].toLowerCase();
        if (subCmd.equals("accept")) {
            privacyManager.setConsent(player.getUniqueId(), true);
            player.sendMessage("&a[VoiceGuard] You have accepted voice monitoring and may play on this server.");
            plugin.getLogger().info(player.getName() + " accepted privacy consent.");
        } else if (subCmd.equals("deny")) {
            privacyManager.setConsent(player.getUniqueId(), false);
            player.kickPlayer(plugin.getConfig().getString("voiceguard.privacy.kick_message", "You must accept voice monitoring to play on this server."));
            plugin.getLogger().info(player.getName() + " denied privacy consent and was kicked.");
        } else {
            sender.sendMessage("Usage: /acceptdeny <accept|deny>");
        }
        return true;
    }
}