package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.BroadcastManager;
import com.Gomania.gChat.managers.MessageManager;
import com.Gomania.gChat.utils.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class BroadcastCommand implements CommandExecutor {

    private final GChat plugin;
    private final BroadcastManager broadcastManager;
    private final MessageManager messages;

    public BroadcastCommand(GChat plugin) {
        this.plugin = plugin;
        this.broadcastManager = plugin.getBroadcastManager();
        this.messages = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.BROADCAST)) {
            sender.sendMessage(messages.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(messages.getMessage("broadcast-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                broadcastManager.reload();
                sender.sendMessage(messages.getMessage("broadcast-reloaded"));
                break;

            case "next":
                broadcastManager.sendNextBroadcastMessage();
                sender.sendMessage(messages.getMessage("broadcast-next-sent"));
                break;

            case "say":
                if (!sender.hasPermission(Permissions.BROADCAST_SAY) && !sender.hasPermission(Permissions.BROADCAST_ALL)) {
                    sender.sendMessage(messages.getMessage("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(messages.getMessage("broadcast-usage-say"));
                    return true;
                }

                String customMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                broadcastManager.sendFormattedBroadcast(customMessage);
                break;

            default:
                try {
                    int index = Integer.parseInt(args[0]) - 1;
                    List<String> broadcastMessages = plugin.getConfigManager().getBroadcastMessages();

                    if (index < 0 || index >= broadcastMessages.size()) {
                        sender.sendMessage(messages.getMessage("broadcast-invalid-index")
                                .replace("{max}", String.valueOf(broadcastMessages.size())));
                        return true;
                    }

                    broadcastManager.sendFormattedBroadcast(broadcastMessages.get(index));
                    sender.sendMessage(messages.getMessage("broadcast-sent-number")
                            .replace("{index}", String.valueOf(index + 1)));

                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.getMessage("broadcast-invalid-argument"));
                }
                break;
        }
        return true;
    }
}