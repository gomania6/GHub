package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Обрабатывает команду /g.
 */
public class GlobalChatCommand implements CommandExecutor {

    private final GChat plugin;

    public GlobalChatCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // Отправить разовое сообщение: /g <сообщение>
            String message = String.join(" ", args);
            plugin.getChatManager().processChatMessage(player, message, "global");
        } else {
            // Переключить режим чата: /g
            plugin.setPlayerChatMode(player.getUniqueId(), "global");
            player.sendMessage(plugin.getMessageManager().getMessage("chat-mode-global"));
        }
        return true;
    }
}