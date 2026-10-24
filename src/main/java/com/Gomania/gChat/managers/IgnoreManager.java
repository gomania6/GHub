package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IgnoreManager {
    private final GChat plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();

    public IgnoreManager(GChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ignores.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать ignores.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // Загружаем данные только когда игрок заходит
    public void loadPlayerCache(UUID uuid) {
        if (config.contains(uuid.toString())) {
            List<String> ignoredStrs = config.getStringList(uuid.toString());
            Set<UUID> ignoredSet = ConcurrentHashMap.newKeySet();
            for (String s : ignoredStrs) {
                try { ignoredSet.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
            ignoredPlayers.put(uuid, ignoredSet);
        }
    }

    // Выгружаем кэш при выходе (Устранение утечки памяти)
    public void removePlayerCache(UUID uuid) {
        ignoredPlayers.remove(uuid);
    }

    public void saveAllOnDisable() {
        for (Map.Entry<UUID, Set<UUID>> entry : ignoredPlayers.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID id : entry.getValue()) list.add(id.toString());
            config.set(entry.getKey().toString(), list);
        }
        synchronized (saveLock) {
            try { config.save(file); } catch (IOException e) {
                plugin.getLogger().warning("Ошибка сохранения ignores.yml");
            }
        }
    }

    private void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAllOnDisable);
    }

    public boolean toggleIgnore(UUID player, UUID target) {
        Set<UUID> ignored = ignoredPlayers.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
        boolean result;
        if (ignored.contains(target)) {
            ignored.remove(target);
            result = false;
        } else {
            ignored.add(target);
            result = true;
        }
        saveAsync();
        return result;
    }

    public boolean isIgnored(UUID player, UUID target) {
        return ignoredPlayers.getOrDefault(player, Collections.emptySet()).contains(target);
    }
}