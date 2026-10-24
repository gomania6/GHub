package com.Gomania.gChat.listeners;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.LuckPermsHook;
import com.Gomania.gChat.hooks.PlaceholderAPIHook;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Управляет событиями входа и выхода игрока.
 */
public class PlayerConnectionListener implements Listener {

    private final GChat plugin;
    private final ConfigManager config;

    public PlayerConnectionListener(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isJoinQuitHandlingEnabled()) return;

        Player player = event.getPlayer();
        plugin.getIgnoreManager().loadPlayerCache(player.getUniqueId());
        // Получаем группу игрока
        String group = null;
        if (plugin.getDependencyManager().hasLuckPerms() && plugin.getLuckPermsHook() != null) {
            group = plugin.getLuckPermsHook().getPlayerGroup(player.getUniqueId(), config.getGroupFormatKeys());
        }

        String joinMessage;
        if (!player.hasPlayedBefore()) {
            joinMessage = config.getFirstJoinMessage(group);
        } else {
            joinMessage = config.getJoinMessage(group);
        }

        // Скрываем стандартное сообщение для всех
        event.setJoinMessage(null);

        if (joinMessage != null && !joinMessage.isEmpty() && !joinMessage.equalsIgnoreCase("none") && !joinMessage.equalsIgnoreCase("null")) {

            // 1. Сначала заменяем наш базовый плейсхолдер
            String rawMessage = joinMessage.replace("%player_name%", player.getName());

            // 2. Прогоняем через PlaceholderAPI (если он установлен)
            PlaceholderAPIHook papiHook = plugin.getPlaceholderAPIHook();
            if (papiHook != null) {
                rawMessage = papiHook.setPlaceholders(player, rawMessage);
            }

            // 3. Только потом применяем цвета и градиенты
            String formatted = ChatColorUtils.translateColors(rawMessage, true, true, true);

            // Рассылаем только тем, у кого включена настройка
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getSettingsManager().getSettings(p.getUniqueId()).showJoinQuitMessages) {
                    p.sendMessage(formatted);
                }
            }
            // Отправляем в консоль
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Скрываем стандартное сообщение
        event.setQuitMessage(null);

        if (config.isJoinQuitHandlingEnabled()) {

            // Получаем группу игрока
            String group = null;
            if (plugin.getDependencyManager().hasLuckPerms() && plugin.getLuckPermsHook() != null) {
                group = plugin.getLuckPermsHook().getPlayerGroup(player.getUniqueId(), config.getGroupFormatKeys());
            }

            String quitMessage = config.getQuitMessage(group);

            if (quitMessage != null && !quitMessage.isEmpty() && !quitMessage.equalsIgnoreCase("none") && !quitMessage.equalsIgnoreCase("null")) {

                // 1. Базовый плейсхолдер
                String rawMessage = quitMessage.replace("%player_name%", player.getName());

                // 2. PlaceholderAPI
                PlaceholderAPIHook papiHook = plugin.getPlaceholderAPIHook();
                if (papiHook != null) {
                    rawMessage = papiHook.setPlaceholders(player, rawMessage);
                }

                // 3. Цвета и градиенты
                String formatted = ChatColorUtils.translateColors(rawMessage, true, true, true);

                // Рассылаем
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getSettingsManager().getSettings(p.getUniqueId()).showJoinQuitMessages) {
                        p.sendMessage(formatted);
                    }
                }
                Bukkit.getConsoleSender().sendMessage(formatted);
            }
        }

        // Очистка кэша LuckPerms
        LuckPermsHook lpHook = plugin.getLuckPermsHook();
        if (lpHook != null) {
            lpHook.removeCache(player.getUniqueId());
        }

        // Сохраняем настройки
        plugin.getSettingsManager().saveSettings(player.getUniqueId());
        // Очистка режима чата
        plugin.removePlayerChatMode(player.getUniqueId());

        // --- ОЧИСТКА УТЕЧЕК ПАМЯТИ ---
        plugin.getChatManager().cleanUpPlayer(player.getUniqueId());
        plugin.getIgnoreManager().removePlayerCache(player.getUniqueId());
        plugin.getSettingsManager().removePlayerFromCache(player.getUniqueId());
    }
}