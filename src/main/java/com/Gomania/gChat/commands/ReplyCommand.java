package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Обрабатывает команду /reply (/r).
 */
public class ReplyCommand implements CommandExecutor {

    private final GChat plugin;

    public ReplyCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getMessage("reply-usage"));
            return true;
        }

        String message = String.join(" ", args);
        plugin.getChatManager().handleReply(player, message);
        return true;
    }
}