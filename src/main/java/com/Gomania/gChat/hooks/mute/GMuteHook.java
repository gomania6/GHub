package com.Gomania.gChat.hooks.mute;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class GMuteHook implements MuteHook {

    private final GChat plugin;
    private boolean isFunctional = false;
    private Method isMutedMethod;

    public GMuteHook(GChat plugin) {
        this.plugin = plugin;

        // Проверяем, установлен ли наш новый плагин на сервере
        if (Bukkit.getPluginManager().getPlugin("gMute") != null) {
            try {
                // Подключаемся к API через рефлексию
                Class<?> apiClass = Class.forName("com.gomania.gMute.api.GMuteAPI");
                isMutedMethod = apiClass.getMethod("isMuted", UUID.class);
                isFunctional = true;
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при инициализации gMute Hook: " + e.getMessage());
            }
        }
    }

    @Override
    public String getPluginName() {
        return "gMute";
    }

    @Override
    public boolean isMuted(Player player) {
        if (!isFunctional || player == null) return false;
        try {
            // Вызываем метод GMuteAPI.isMuted(UUID)
            return (boolean) isMutedMethod.invoke(null, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
}