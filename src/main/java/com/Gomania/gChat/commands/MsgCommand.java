package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Обрабатывает команду /msg.
 */
public class MsgCommand implements CommandExecutor {

    private final GChat plugin;

    public MsgCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("players-only"));
            return true;
        }

        if (!plugin.getConfigManager().isPrivateMessageEnabled()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("pm-disabled"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage-msg"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getChatManager().handlePrivateMessage(player, target, message);

        return true;
    }
}