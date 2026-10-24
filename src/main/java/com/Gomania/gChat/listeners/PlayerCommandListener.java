package com.Gomania.gChat.listeners;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Блокирует выполнение команд, указанных в config.yml.
 */
public class PlayerCommandListener implements Listener {

    private final ConfigManager config;
    private final MessageManager messages;

    public PlayerCommandListener(GChat plugin) {
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessageManager();
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!config.isBlockCommandsEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        if (player.isOp() || player.hasPermission("gchat.bcomm.bypass")) {
            return;
        }

        List<String> blockedCommands = config.getBlockedCommands();
        String cleanMessage = message.startsWith("/") ? message.substring(1) : message;

        for (String cmd : blockedCommands) {
            String cleanCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;

            // Проверяем, что команда начинается с запрещенной ИЛИ точно ей равна
            if (cleanMessage.startsWith(cleanCmd.toLowerCase() + " ") ||
                    cleanMessage.equals(cleanCmd.toLowerCase())) {

                player.sendMessage(messages.getMessage("no-permission"));
                event.setCancelled(true);
                return;
            }
        }
    }
}