package com.Gomania.gChat.hooks.mute;

import org.bukkit.entity.Player;

/**
 * Общий интерфейс для проверки мута в разных плагинах.
 */
public interface MuteHook {

    /**
     * Возвращает название плагина наказаний.
     */
    String getPluginName();

    /**
     * Проверяет, заглушен ли игрок.
     * @param player Игрок
     * @return true, если игрок в муте
     */
    boolean isMuted(Player player);
}