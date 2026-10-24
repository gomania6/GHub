package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;

public class AuthMeHook {

    private final GChat plugin;
    private boolean isFunctional = false;
    private Object authMeApiInstance;
    private Method isAuthenticatedMethod;

    public AuthMeHook(GChat plugin) {
        this.plugin = plugin;
        try {
            // Загружаем класс AuthMeApi через рефлексию
            Class<?> authMeClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");

            // Получаем метод getInstance() и вызываем его
            Method getInstanceMethod = authMeClass.getMethod("getInstance");
            authMeApiInstance = getInstanceMethod.invoke(null);

            // Получаем метод isAuthenticated(Player)
            isAuthenticatedMethod = authMeClass.getMethod("isAuthenticated", Player.class);

            isFunctional = true;
        } catch (Exception e) {
            plugin.getLogger().warning("AuthMe API не найдено или устарело. Защита команд отключена.");
        }
    }

    /**
     * Проверяет, авторизован ли игрок.
     * Если AuthMe нет на сервере, всегда возвращает true (игрок может писать).
     */
    public boolean isAuthenticated(Player player) {
        if (!isFunctional || player == null) return true;

        try {
            // Динамически вызываем проверку авторизации
            return (boolean) isAuthenticatedMethod.invoke(authMeApiInstance, player);
        } catch (Exception e) {
            return true; // На случай непредвиденных ошибок API
        }
    }
}