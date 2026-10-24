package com.Gomania.gChat.listeners;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.ChatManager;
import com.Gomania.gChat.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerChatListener implements Listener {

    private final GChat plugin;
    private final ChatManager chatManager;
    private final ConfigManager configManager;

    public PlayerChatListener(GChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        if (!configManager.isPluginEnabled()) return;

        Player player = event.getPlayer();
        String rawMessage = event.getMessage();
        String chatMode = plugin.getPlayerChatMode(player.getUniqueId());

        if (rawMessage.startsWith("!")) {
            rawMessage = rawMessage.substring(1).trim();
            if (rawMessage.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            chatMode = "global";
        } else {
            chatMode = chatMode.toLowerCase();
        }

        event.setCancelled(true);

        final String finalMessage = rawMessage;
        final String finalMode = chatMode;

        // Переносим выполнение в основной поток сервера (Paper thread-safety):
        // Безопасное чтение инвентаря, локаций (локальный чат) и PlaceholderAPI
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            ItemStack item = null;
            if (configManager.isItemShowcaseEnabled() && finalMessage.contains(configManager.getItemShowcasePlaceholder())) {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand != null && inHand.getType() != Material.AIR) {
                    item = inHand.clone();
                }
            }

            chatManager.processChatMessage(player, finalMessage, finalMode, item);
        });
    }
}