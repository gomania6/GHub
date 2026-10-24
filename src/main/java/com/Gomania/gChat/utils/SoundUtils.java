package com.Gomania.gChat.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class SoundUtils {

    private SoundUtils() {}

    /**
     * Универсальный поиск звука через Registry.SOUNDS для совместимости с 1.20 - 1.21+ / 26.3.
     * Поддерживает как старый формат (BLOCK_NOTE_BLOCK_PLING), так и новый (block.note_block.pling).
     */
    public static Sound getSound(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String clean = name.trim().toLowerCase(Locale.ROOT);

        // 1. Прямой поиск (если указан формат с точками, например: entity.experience_orb.pickup)
        NamespacedKey directKey = NamespacedKey.minecraft(clean);
        Sound sound = Registry.SOUNDS.get(directKey);
        if (sound != null) {
            return sound;
        }

        // 2. Конвертация старого Enum-имени (ENTITY_EXPERIENCE_ORB_PICKUP -> entity.experience_orb.pickup)
        String dotFormat = clean.replace('_', '.');
        Sound dotSound = Registry.SOUNDS.get(NamespacedKey.minecraft(dotFormat));
        if (dotSound != null) {
            return dotSound;
        }

        // 3. Поиск по совпадению ключа в реестре (для составных имен вроде note_block, ender_dragon, experience_orb)
        String normalizedTarget = clean.replace(".", "").replace("_", "");
        for (Sound registeredSound : Registry.SOUNDS) {
            NamespacedKey key = registeredSound.getKey();
            if (key != null) {
                String normalizedKey = key.getKey().replace(".", "").replace("_", "");
                if (normalizedKey.equals(normalizedTarget)) {
                    return registeredSound;
                }
            }
        }

        return null;
    }
}