package com.Gomania.gChat;

import com.Gomania.gChat.commands.*;
import com.Gomania.gChat.hooks.AuthMeHook;
import com.Gomania.gChat.hooks.DiscordSRVHook;
import com.Gomania.gChat.hooks.LuckPermsHook;
import com.Gomania.gChat.hooks.PlaceholderAPIHook;
import com.Gomania.gChat.hooks.TownyHook;
import com.Gomania.gChat.listeners.PlayerAdvancementListener;
import com.Gomania.gChat.listeners.PlayerChatListener;
import com.Gomania.gChat.listeners.PlayerCommandListener;
import com.Gomania.gChat.listeners.PlayerConnectionListener;
import com.Gomania.gChat.listeners.PlayerDeathListener;
import com.Gomania.gChat.managers.*;
import com.Gomania.gChat.utils.UpdateChecker;

import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GChat extends JavaPlugin {

    // Константа ID проекта на Modrinth
    public static final String MODRINTH_PROJECT_ID = "gchat-plugin";

    // Менеджеры
    private AuthMeHook authMeHook;
    private static GChat instance;
    private com.Gomania.gChat.api.SettingsGuiProvider settingsGuiProvider = null;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ChatManager chatManager;
    private BroadcastManager broadcastManager;
    private DependencyManager dependencyManager;
    private LogManager logManager;
    private ChannelManager channelManager;
    private ChatBubbleManager chatBubbleManager;
    private MuteManager muteManager;
    private IgnoreManager ignoreManager;
    private PlayerSettingsManager settingsManager;

    private UpdateChecker updateChecker;

    // Хуки
    private LuckPermsHook luckPermsHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private DiscordSRVHook discordSRVHook;
    private TownyHook townyHook;

    private final Map<UUID, String> playerChatModes = new HashMap<>();

    @Override
    public void onEnable() {
        // 1. Базовая инициализация
        instance = this;
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        dependencyManager = new DependencyManager(this);
        logManager = new LogManager(this);
        settingsManager = new PlayerSettingsManager(this);

        // Инициализация менеджера игнора ДО ChatManager
        ignoreManager = new IgnoreManager(this);

        // 2. Проверка зависимостей
        dependencyManager.checkDependencies();

        if (dependencyManager.hasLuckPerms()) {
            luckPermsHook = new LuckPermsHook(this, dependencyManager.getLuckPermsAPI());
        }
        if (dependencyManager.hasPlaceholderAPI()) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            new com.Gomania.gChat.placeholders.GChatExpansion(this).register();
        }
        if (dependencyManager.hasDiscordSRV() && configManager.isDiscordEnabled()) {
            discordSRVHook = new DiscordSRVHook(this);
            discordSRVHook.subscribe();
        }

        muteManager = new MuteManager(this);

        if (dependencyManager.hasTowny() && configManager.isTownyEnabled()) {
            townyHook = new TownyHook(this);
            getLogger().info("Towny integration enabled.");
        }

        if (dependencyManager.hasAuthMe()) {
            authMeHook = new AuthMeHook(this);
        }

        // 3. Функциональные менеджеры
        chatBubbleManager = new ChatBubbleManager(this);
        chatManager = new ChatManager(this);
        broadcastManager = new BroadcastManager(this);
        broadcastManager.scheduleBroadcasts();
        channelManager = new ChannelManager(this);

        // 4. Регистрация событий и команд
        registerListeners();
        registerCommands();

        // ==========================================
        // 5. ИНТЕГРАЦИИ (bStats & UpdateChecker)
        // ==========================================

        int bStatsId = 30690;
        try {
            new Metrics(this, bStatsId);
            getLogger().info("bStats метрики успешно подключены!");
        } catch (Exception e) {
            getLogger().warning("Не удалось запустить bStats.");
        }

        String modrinthProjectId = getConfig().getString("update-checker.modrinth-project-id", MODRINTH_PROJECT_ID);
        updateChecker = new UpdateChecker(this, modrinthProjectId);

        if (getConfig().getBoolean("update-checker.enabled", true)) {
            updateChecker.checkForUpdates(hasUpdate -> {
                if (hasUpdate) {
                    getLogger().warning("========================================");
                    getLogger().warning("A new version of gChat is available!");
                    getLogger().warning("Current: " + getDescription().getVersion() + " | Latest: " + updateChecker.getLatestVersion());
                    getLogger().warning("Download: https://modrinth.com/plugin/" + modrinthProjectId);
                    getLogger().warning("========================================");
                }
            });
        }

        // Уведомление администраторов при входе
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onAdminJoin(PlayerJoinEvent event) {
                if (event.getPlayer().hasPermission("gchat.admin") && updateChecker != null && updateChecker.isUpdateAvailable()) {
                    event.getPlayer().sendMessage("§8[§egChat§8] §aA new version of gChat is available: §f" + updateChecker.getLatestVersion());
                    event.getPlayer().sendMessage("§8[§egChat§8] §7Download: §ehttps://modrinth.com/plugin/" + modrinthProjectId);
                }
            }
        }, this);

        getLogger().info("gChat plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (broadcastManager != null) broadcastManager.cancelBroadcasts();
        if (chatBubbleManager != null) chatBubbleManager.shutdown();
        if (channelManager != null) channelManager.unregisterAll();
        if (discordSRVHook != null) discordSRVHook.unsubscribe();
        if (luckPermsHook != null) luckPermsHook.clearCache();

        // Сохраняем все данные и сбрасываем буфер логов при /stop или /reload
        if (settingsManager != null) settingsManager.saveAllOnDisable();
        if (ignoreManager != null) ignoreManager.saveAllOnDisable();
        if (logManager != null) logManager.onDisable();

        playerChatModes.clear();
        getLogger().info("gChat plugin disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerAdvancementListener(this), this);
    }

    private void registerCommands() {
        registerCommand("gchat", new GChatCommand(this));
        registerCommand("g", new GlobalChatCommand(this));
        registerCommand("l", new LocalChatCommand(this));
        registerCommand("broadcast", new BroadcastCommand(this));
        registerCommand("msg", new MsgCommand(this));
        registerCommand("reply", new ReplyCommand(this));
        registerCommand("ignore", new IgnoreCommand(this));
        registerCommand("settings", new SettingsCommand(this));
        registerCommand("mutechat", new MuteChatCommand(this));
        registerCommand("clearchat", new ClearChatCommand(this));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                command.setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        }
    }

    public void reloadPlugin() {
        if (channelManager != null) channelManager.unregisterAll();
        // Очищаем баблы без пересоздания слушателя событий (фикс утечки памяти)
        if (chatBubbleManager != null) chatBubbleManager.cleanup();

        configManager.reloadConfigs();
        messageManager.reloadMessages();
        if (logManager != null) logManager.reloadFormatter();
        dependencyManager.checkDependencies();

        if (dependencyManager.hasLuckPerms()) {
            if (luckPermsHook == null) luckPermsHook = new LuckPermsHook(this, dependencyManager.getLuckPermsAPI());
            else luckPermsHook.clearCache();
        }
        if (dependencyManager.hasPlaceholderAPI() && placeholderAPIHook == null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
        }

        if (muteManager == null) muteManager = new MuteManager(this);
        else muteManager.reload();

        boolean discordShouldBeEnabled = dependencyManager.hasDiscordSRV() && configManager.isDiscordEnabled();
        if (discordShouldBeEnabled && discordSRVHook == null) {
            discordSRVHook = new DiscordSRVHook(this);
            discordSRVHook.subscribe();
        } else if (!discordShouldBeEnabled && discordSRVHook != null) {
            discordSRVHook.unsubscribe();
            discordSRVHook = null;
        }

        if (dependencyManager.hasAuthMe() && authMeHook == null) {
            authMeHook = new AuthMeHook(this);
        } else if (!dependencyManager.hasAuthMe()) {
            authMeHook = null;
        }

        boolean townyShouldBeEnabled = dependencyManager.hasTowny() && configManager.isTownyEnabled();
        if (townyShouldBeEnabled && townyHook == null) {
            townyHook = new TownyHook(this);
        } else if (!townyShouldBeEnabled) {
            townyHook = null;
        }

        if (chatBubbleManager == null) {
            chatBubbleManager = new ChatBubbleManager(this);
        }
        chatManager.reload();
        broadcastManager.reload();
        channelManager.loadChannels();
    }

    // --- Геттеры ---
    public AuthMeHook getAuthMeHook() { return authMeHook; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ChatManager getChatManager() { return chatManager; }
    public BroadcastManager getBroadcastManager() { return broadcastManager; }
    public DependencyManager getDependencyManager() { return dependencyManager; }
    public LogManager getLogManager() { return logManager; }
    public ChannelManager getChannelManager() { return channelManager; }
    public ChatBubbleManager getChatBubbleManager() { return chatBubbleManager; }
    public MuteManager getMuteManager() { return muteManager; }
    public IgnoreManager getIgnoreManager() { return ignoreManager; }
    public PlayerSettingsManager getSettingsManager() { return settingsManager; }

    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }
    public DiscordSRVHook getDiscordSRVHook() { return discordSRVHook; }
    public TownyHook getTownyHook() { return townyHook; }

    public String getPlayerChatMode(UUID uuid) {
        return playerChatModes.getOrDefault(uuid, configManager.getDefaultChatMode());
    }
    public void setPlayerChatMode(UUID uuid, String mode) {
        playerChatModes.put(uuid, mode);
    }
    public void removePlayerChatMode(UUID uuid) {
        playerChatModes.remove(uuid);
    }

    public static GChat getInstance() { return instance; }

    public com.Gomania.gChat.api.SettingsGuiProvider getSettingsGuiProvider() {
        return settingsGuiProvider;
    }

    public void setSettingsGuiProvider(com.Gomania.gChat.api.SettingsGuiProvider provider) {
        this.settingsGuiProvider = provider;
    }
}