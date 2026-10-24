package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.utils.ChatColorUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэширующий хук для API LuckPerms.
 * Управляет получением групп и суффиксов.
 */
public class LuckPermsHook {

    private final GChat plugin;
    private final Map<UUID, CachedPlayerData> cache = new ConcurrentHashMap<>();
    private final Object luckPermsAPI;

    // Внутренний класс для хранения кэша
    private static class CachedPlayerData {
        String primaryGroup;
        String prefix;
        String suffix;
        long timestamp; // Время создания кэша
    }

    public LuckPermsHook(GChat plugin, Object luckPermsAPI) {
        this.plugin = plugin;
        this.luckPermsAPI = luckPermsAPI;
    }

    /**
     * Получает основную группу игрока, которая есть в 'validGroups'.
     */
    public String getPlayerGroup(UUID uuid, Set<String> validGroups) {
        long now = System.currentTimeMillis();
        CachedPlayerData cached = cache.get(uuid);

        // Проверяем кэш (кэш живет 1 минуту)
        if (cached != null && now - cached.timestamp < 60000) {
            return cached.primaryGroup;
        }

        // Кэша нет или он устарел, делаем запрос к API
        try {
            Class<?> apiClass = luckPermsAPI.getClass();
            Object userManager = apiClass.getMethod("getUserManager").invoke(luckPermsAPI);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) return null; // Игрок не загружен в LP

            String primaryGroup = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);

            // Создаем новые кэшированные данные
            CachedPlayerData newData = new CachedPlayerData();
            newData.timestamp = now;

            // Проверяем, есть ли эта группа в наших конфигах
            if (primaryGroup != null && validGroups.contains(primaryGroup.toLowerCase())) {
                newData.primaryGroup = primaryGroup.toLowerCase();
            } else {
                newData.primaryGroup = null; // или "default", если нужно
            }

            // Получаем и кэшируем суффикс и префикс
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);

            String suffix = (String) metaData.getClass().getMethod("getSuffix").invoke(metaData);
            String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);

            newData.suffix = (suffix != null) ? ChatColorUtils.translateColors(suffix, true, true, true) : "";
            newData.prefix = (prefix != null) ? ChatColorUtils.translateColors(prefix, true, true, true) : "";

            cache.put(uuid, newData);
            return newData.primaryGroup;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get LuckPerms data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Получает префикс игрока из кэша.
     */
    public String getPlayerPrefix(UUID uuid) {
        CachedPlayerData cached = cache.get(uuid);
        if (cached == null) {
            getPlayerGroup(uuid, plugin.getConfigManager().getGroupFormatKeys());
            cached = cache.get(uuid);
        }
        return (cached != null) ? cached.prefix : "";
    }

    /**
     * Получает суффикс игрока из кэша.
     */
    public String getPlayerSuffix(UUID uuid) {
        CachedPlayerData cached = cache.get(uuid);
        // Если кэша нет, getPlayerGroup() его создаст
        if (cached == null) {
            getPlayerGroup(uuid, plugin.getConfigManager().getGroupFormatKeys());
            cached = cache.get(uuid); // Повторный запрос
        }
        return (cached != null) ? cached.suffix : "";
    }

    public void removeCache(UUID uuid) {
        cache.remove(uuid);
    }

    public void clearCache() {
        cache.clear();
    }
}