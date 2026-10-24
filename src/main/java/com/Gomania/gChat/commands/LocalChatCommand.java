package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Обрабатывает команду /l.
 */
public class LocalChatCommand implements CommandExecutor {

    private final GChat plugin;

    public LocalChatCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;

        // --- ЗАЩИТА AUTHME ---
        if (plugin.getAuthMeHook() != null && !plugin.getAuthMeHook().isAuthenticated(player)) {
            if (args.length > 0) {
                player.performCommand("login " + String.join(" ", args));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("auth-required"));
            }
            return true;
        }
        // ---------------------

        if (args.length > 0) {
            String message = String.join(" ", args);
            plugin.getChatManager().processChatMessage(player, message, "local");
        } else {
            plugin.setPlayerChatMode(player.getUniqueId(), "local");
            player.sendMessage(plugin.getMessageManager().getMessage("chat-mode-local"));
        }
        return true;
    }
}