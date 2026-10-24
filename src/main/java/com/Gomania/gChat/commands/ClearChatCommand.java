package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand implements CommandExecutor {

    private final GChat plugin;

    public ClearChatCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gchat.clearchat")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("gchat.clearchat.bypass")) {
                for (int i = 0; i < 100; i++) {
                    p.sendMessage("");
                }
            }
            String clearMsg = plugin.getMessageManager().getMessage("chat-cleared").replace("%admin%", sender.getName());
            p.sendMessage(clearMsg);
        }

        plugin.getLogger().info("Чат был очищен игроком/консолью " + sender.getName());
        return true;
    }
}