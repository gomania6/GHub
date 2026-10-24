package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MuteChatCommand implements CommandExecutor {

    private final GChat plugin;

    public MuteChatCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gchat.mutechat")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        boolean isMuted = plugin.getChatManager().toggleChatMute();

        if (isMuted) {
            Bukkit.broadcastMessage(plugin.getMessageManager().getMessage("chat-muted-broadcast"));
        } else {
            Bukkit.broadcastMessage(plugin.getMessageManager().getMessage("chat-unmuted-broadcast"));
        }

        return true;
    }
}