package com.Gomania.gChat.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет тайм-аутами для игроков.
 */
public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Проверяет, прошел ли кулдаун, и сбрасывает его.
     * @param uuid UUID игрока
     * @param seconds Длительность кулдауна в секундах
     * @return true, если кулдаун прошел, иначе false
     */
    public boolean check(UUID uuid, int seconds) {
        long now = System.currentTimeMillis();
        Long lastTime = cooldowns.get(uuid);

        if (lastTime == null || now - lastTime >= seconds * 1000L) {
            cooldowns.put(uuid, now);
            return true;
        }
        return false;
    }

    /**
     * Получает оставшееся время кулдауна в секундах.
     * @param uuid UUID игрока
     * @param seconds Длительность кулдауна
     * @return Оставшееся время в секундах
     */
    public long getRemaining(UUID uuid, int seconds) {
        Long lastTime = cooldowns.get(uuid);
        if (lastTime == null) return 0;

        long elapsed = System.currentTimeMillis() - lastTime;
        long remaining = seconds * 1000L - elapsed;

        return remaining > 0 ? (remaining / 1000L) + 1 : 0; // +1 для округления вверх
    }

    /**
     * Очищает все кулдауны.
     */
    public void clear() {
        cooldowns.clear();
    }

    /**
     * Удаляет кулдаун для конкретного игрока.
     * @param uuid UUID игрока
     */
    public void remove(UUID uuid) {
        cooldowns.remove(uuid);
    }
}