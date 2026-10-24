package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnoreCommand implements CommandExecutor {
    private final GChat plugin;

    public IgnoreCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getMessage("ignore-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(plugin.getMessageManager().getMessage("ignore-self"));
            return true;
        }

        if (target.hasPermission("gchat.ignore.bypass")) {
            player.sendMessage(plugin.getMessageManager().getMessage("ignore-bypass"));
            return true;
        }

        boolean isIgnored = plugin.getIgnoreManager().toggleIgnore(player.getUniqueId(), target.getUniqueId());
        if (isIgnored) {
            player.sendMessage(plugin.getMessageManager().getMessage("ignore-added").replace("%player%", target.getName()));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("ignore-removed").replace("%player%", target.getName()));
        }
        return true;
    }
}