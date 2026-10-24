package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Хук для EssentialsX.
 * Используется для проверки статуса мута (Mute).
 */
public class EssentialsHook {

    private final GChat plugin;
    private final Object essentialsPlugin;
    private boolean isFunctional = false;

    public EssentialsHook(GChat plugin) {
        this.plugin = plugin;
        this.essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");

        if (essentialsPlugin != null) {
            isFunctional = true;
            plugin.getLogger().info("Essentials hook initialized (Mute support enabled).");
        }
    }

    /**
     * Проверяет, находится ли игрок в муте через Essentials.
     * @param player Игрок
     * @return true, если игрок заглушен
     */
    public boolean isMuted(Player player) {
        if (!isFunctional || player == null) return false;

        try {
            // Рефлексия: user = ess.getUser(player); return user.isMuted();
            Class<?> essClass = essentialsPlugin.getClass();
            Object user = essClass.getMethod("getUser", Player.class).invoke(essentialsPlugin, player);

            if (user != null) {
                return (boolean) user.getClass().getMethod("isMuted").invoke(user);
            }
        } catch (Exception e) {
            // Логируем только debug, чтобы не спамить в консоль при ошибках
        }

        return false;
    }
}