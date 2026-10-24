package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Управляет файлом messages_xx.yml, загрузкой и форматированием сообщений.
 */
public class MessageManager {

    private final GChat plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private String prefix; // Кэшируем префикс

    public MessageManager(GChat plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        // [ ИСПОЛЬЗУЕТ ИСПРАВЛЕНИЕ ИЗ ConfigManager ]
        // Мы просто используем новый геттер. ConfigManager всегда загружается раньше,
        // чем MessageManager (согласно GChat.java), поэтому эта проверка безопасна.
        String language = plugin.getConfigManager().getLanguage();

        messagesFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");

        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResource("messages_" + language + ".yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                } else {
                    plugin.getLogger().warning("Could not find messages_" + language + ".yml resource. Creating empty file.");
                    messagesFile.createNewFile();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create messages file: " + e.getMessage());
            }
        }

        reloadMessages();
    }

    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messagesConfig.getString("prefix", "&8[&6gChat&8]");
    }

    /**
     * Получает форматированное сообщение из конфига.
     * @param path Путь к сообщению (e.g. "no-permission")
     * @return Форматированное сообщение с цветами и префиксом.
     */
    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "§cMessage not found: " + path;
        }

        message = message.replace("{prefix}", prefix);
        return ChatColorUtils.translateColors(message, true, true);
    }

    /**
     * Получает список строк для /gchat help.
     * @return Список форматированных строк.
     */
    public List<String> getHelpMessage() {
        List<String> helpLines = messagesConfig.getStringList("help");
        if (helpLines == null || helpLines.isEmpty()) {
            // Fallback, если в messages.yml нет секции help
            helpLines = new ArrayList<>();
            helpLines.add("&6&l=== gChat Help ===");
            helpLines.add("&e/gchat reload &7- Перезагрузить конфигурацию");
            helpLines.add("&e/gchat help &7- Показать эту помощь");
            helpLines.add("&e/gchat debug &7- Debug информация");
            helpLines.add("&e/g &7- Переключиться на глобальный чат");
            helpLines.add("&e/l &7- Переключиться на локальный чат");
            helpLines.add("&e!<сообщение> &7- Отправить разовое глобальное сообщение");
        }

        return helpLines.stream()
                .map(line -> ChatColorUtils.translateColors(line, true, true))
                .collect(Collectors.toList());
    }
}