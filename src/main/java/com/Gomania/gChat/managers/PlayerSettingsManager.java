package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSettingsManager {

    private final GChat plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();

    // Блокировка для потокобезопасного сохранения файла
    private final Object saveLock = new Object();

    public PlayerSettingsManager(GChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_settings.yml");
        loadConfigs();
    }

    private void loadConfigs() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать player_settings.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // Получаем настройки игрока
    public PlayerSettings getSettings(UUID uuid) {
        return settingsCache.computeIfAbsent(uuid, this::loadFromConfig);
    }

    private PlayerSettings loadFromConfig(UUID uuid) {
        String path = uuid.toString();
        PlayerSettings settings = new PlayerSettings();

        if (config.contains(path)) {
            settings.showDeathMessages = config.getBoolean(path + ".show-deaths", true);
            settings.showJoinQuitMessages = config.getBoolean(path + ".show-join-quit", true);
            settings.globalChatEnabled = config.getBoolean(path + ".global-chat", true);
            settings.chatColorTheme = config.getString(path + ".chat-color", "");
            settings.chatColorMode = config.getString(path + ".chat-color-mode", "chat");
            settings.showAdvancements = config.getBoolean(path + ".show-advancements", true);
            settings.mentionSound = config.getString(path + ".mention-sound", "DEFAULT");
        }
        return settings;
    }

    // Сохраняем настройки
    public void saveSettings(UUID uuid) {
        PlayerSettings settings = settingsCache.get(uuid);
        if (settings == null) return;

        String path = uuid.toString();
        config.set(path + ".show-deaths", settings.showDeathMessages);
        config.set(path + ".show-join-quit", settings.showJoinQuitMessages);
        config.set(path + ".global-chat", settings.globalChatEnabled);
        config.set(path + ".chat-color", settings.chatColorTheme);
        config.set(path + ".chat-color-mode", settings.chatColorMode);
        config.set(path + ".show-advancements", settings.showAdvancements);
        config.set(path + ".mention-sound", settings.mentionSound);

        saveFileAsync();
    }

    private void saveFileAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (saveLock) {
                try {
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().warning("Не удалось сохранить player_settings.yml: " + e.getMessage());
                }
            }
        });
    }

    // Синхронное сохранение всех данных (используется при выключении плагина)
    public void saveAllOnDisable() {
        for (Map.Entry<UUID, PlayerSettings> entry : settingsCache.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerSettings settings = entry.getValue();
            String path = uuid.toString();

            config.set(path + ".show-deaths", settings.showDeathMessages);
            config.set(path + ".show-join-quit", settings.showJoinQuitMessages);
            config.set(path + ".global-chat", settings.globalChatEnabled);
            config.set(path + ".chat-color", settings.chatColorTheme);
            config.set(path + ".chat-color-mode", settings.chatColorMode);
            config.set(path + ".show-advancements", settings.showAdvancements);
            config.set(path + ".mention-sound", settings.mentionSound);
        }

        synchronized (saveLock) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка сохранения при выключении: " + e.getMessage());
            }
        }
    }

    // Очистка кэша игрока при выходе
    public void removePlayerFromCache(UUID uuid) {
        settingsCache.remove(uuid);
    }

    // Внутренний класс для хранения данных
    public static class PlayerSettings {
        public boolean showDeathMessages = true;
        public boolean showJoinQuitMessages = true;
        public boolean globalChatEnabled = true;
        public String chatColorTheme = "";
        public String chatColorMode = "chat";
        public boolean showAdvancements = true;
        public String mentionSound = "DEFAULT";
    }
}