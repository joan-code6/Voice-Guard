package de.joancode;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class PrivacyCommand implements CommandExecutor, TabCompleter {

    private final VoiceGuard plugin;

    public PrivacyCommand(VoiceGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>Error</bold></red> This command can only be used by players."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("accept")) {
            plugin.getPrivacyManager().setConsent(player.getUniqueId(), true);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            // Reset game mode if it was changed
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            }
            final Component PrivacyAccepted = MiniMessage.miniMessage().deserialize(
                "<red><bold>Privacy</bold></red> You accepted our GDPR policy. You can opt out at any Moment using /privacy opt-out."
            );
            sender.sendMessage(PrivacyAccepted);
        }

        else if (args.length > 0 && (args[0].equalsIgnoreCase("deny") || args[0].equalsIgnoreCase("opt-out"))) {
            plugin.getPrivacyManager().revokeConsent(player.getUniqueId());
            final Component PrivacyOptedOut = MiniMessage.miniMessage().deserialize(
                "<red><bold>Privacy</bold></red> You opted out of our GDPR policy."
            );
            sender.sendMessage(PrivacyOptedOut);
            // Kick the player
            player.kick(Component.text("You can only play on this server when accepting the TOS. Use /privacy accept when you rejoin."));
            return true;
        }

        else if (args.length > 0 && args[0].equalsIgnoreCase("tos")) {
            String tosText = plugin.getConfig().getString("voiceguard.privacy.tos_text", "Terms of Service not configured.");
            final Component tosMessage = MiniMessage.miniMessage().deserialize(tosText);
            sender.sendMessage(tosMessage);
            return true;
        }

        else if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            final Component PrivacyInfo = MiniMessage.miniMessage().deserialize(
                "<red><bold>Privacy</bold></red> VoiceGuard collects voice data to provide its services. " +
                "By using this plugin, you agree to our <click:open_url:'https://example.com/privacy'>Privacy Policy</click>.\n" +
                "To accept, use <green>/privacy accept</green>.\n" +
                "To opt-out, use <green>/privacy deny</green> or <green>/privacy opt-out</green>.\n" +
                "To view TOS, use <green>/privacy tos</green>."
            );

            sender.sendMessage(PrivacyInfo);
            return true;
        }

        else if (args[0].equalsIgnoreCase("help")) {
            final Component PrivacyHelp = MiniMessage.miniMessage().deserialize(
                "<red><bold>Privacy Command Help</bold></red>\n" +
                "<green>/privacy info</green> - Show privacy information.\n" +
                "<green>/privacy accept</green> - Accept the privacy policy.\n" +
                "<green>/privacy deny</green> or <green>/privacy opt-out</green> - Opt out and leave the server.\n" +
                "<green>/privacy tos</green> - Show the full Terms of Service.\n" +
                "<green>/privacy help</green> - Show this help message."
            );

            sender.sendMessage(PrivacyHelp);
            return true;
        }

        else if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("voiceguard.admin")) {
                final Component NoPermission = MiniMessage.miniMessage().deserialize(
                    "<red><bold>Error</bold></red> You do not have permission to execute this command."
                );
                sender.sendMessage(NoPermission);
                return true;
            }

            plugin.reloadConfig();
            final Component Reloaded = MiniMessage.miniMessage().deserialize(
                "<green><bold>Success</bold></green> VoiceGuard configuration reloaded."
            );
            sender.sendMessage(Reloaded);
        } else {
            final Component InvalidArg = MiniMessage.miniMessage().deserialize(
                "<red><bold>Error</bold></red> Invalid argument. Use /privacy help for help."
            );
            sender.sendMessage(InvalidArg);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@Nullable CommandSender sender, @Nullable Command command, @Nullable String alias, @Nullable String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : new String[]{"accept", "deny", "opt-out", "tos", "info", "help", "reload"}) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return null;
    }
}