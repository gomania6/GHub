package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import org.bukkit.entity.Player;

/**
 * Хук для PlaceholderAPI.
 * Применяет плейсхолдеры к строке.
 */
public class PlaceholderAPIHook {

    private final GChat plugin;

    public PlaceholderAPIHook(GChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Заменяет PAPI плейсхолдеры в строке.
     * @param player Игрок, для которого применяются плейсхолдеры
     * @param text Текст
     * @return Текст с плейсхолдерами
     */
    public String setPlaceholders(Player player, String text) {
        // Проверка уже сделана в DependencyManager, но дублируем на всякий случай
        if (!plugin.getDependencyManager().hasPlaceholderAPI() || text == null || text.isEmpty()) {
            return text;
        }

        try {
            // Используем рефлексию, чтобы не было 'hard' зависимости
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, text);

            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set PlaceholderAPI placeholders for " + player.getName() + ": " + e.getMessage());
        }

        return text;
    }
}