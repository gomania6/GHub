package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.concurrent.ThreadLocalRandom;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final GChat plugin;
    private FileConfiguration config;
    private FileConfiguration broadcastConfig;
    private FileConfiguration integrationsConfig;

    private File broadcastFile;
    private File integrationsFile;

    private List<String> blockedCommands;
    private boolean logToConsole;
    private boolean blockCommandsEnabled;
    private String language;

    public ConfigManager(GChat plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        broadcastFile = new File(plugin.getDataFolder(), "broadcast.yml");
        if (!broadcastFile.exists()) {
            plugin.saveResource("broadcast.yml", false);
        }
        broadcastConfig = YamlConfiguration.loadConfiguration(broadcastFile);

        integrationsFile = new File(plugin.getDataFolder(), "integrations.yml");
        if (!integrationsFile.exists()) {
            plugin.saveResource("integrations.yml", false);
        }
        integrationsConfig = YamlConfiguration.loadConfiguration(integrationsFile);

        cacheValues();
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    private void cacheValues() {
        this.language = config.getString("language", "ru");
        this.logToConsole = config.getBoolean("log-to-console", true);
        this.blockedCommands = config.getStringList("blocked-commands");
        this.blockCommandsEnabled = !blockedCommands.isEmpty();
    }

    // --- Общее ---
    public boolean isPluginEnabled() { return config.getBoolean("enabled", true); }
    public String getLanguage() { return language; }
    public String getDefaultChatMode() { return config.getString("default-chat-mode", "local"); }
    public boolean isLogToConsoleEnabled() { return logToConsole; }

    // --- МОДЕРАЦИЯ ---
    public boolean isModBlockLinks() { return config.getBoolean("moderation.block-links", true); }
    public boolean isModBlockIps() { return config.getBoolean("moderation.block-ips", true); }
    public boolean isAntiCapsEnabled() { return config.getBoolean("moderation.anti-caps.enabled", true); }
    public int getAntiCapsMinLength() { return config.getInt("moderation.anti-caps.min-length", 5); }
    public int getAntiCapsMaxPercentage() { return config.getInt("moderation.anti-caps.max-percentage", 70); }

    // --- ЧАТ БАБЛЫ ---
    public boolean isChatBubblesEnabled() { return config.getBoolean("chat-bubbles.enabled", true); }
    public String getChatBubbleFormat() { return config.getString("chat-bubbles.format", "&f%message%"); }
    public int getChatBubbleTime() { return config.getInt("chat-bubbles.display-time", 5); }
    public double getChatBubbleHeight() { return config.getDouble("chat-bubbles.height-offset", 2.3); }
    public boolean isChatBubbleStripColors() { return config.getBoolean("chat-bubbles.strip-colors", false); }

    // --- Локальный/Глобальный ---
    public boolean isLocalChatEnabled() { return config.getBoolean("local-chat.enabled", true); }
    public int getLocalChatRadius() { return config.getInt("local-chat.radius", 100); }
    public String getLocalChatFormat() { return config.getString("local-chat.format", "&8[L]"); }
    public boolean showLocalChatSilentMessage() { return config.getBoolean("local-chat.show-silent-message", true); }
    public boolean showDisabledLocalChatMessage() { return config.getBoolean("local-chat.show-disabled-local-chat", true); }

    public boolean isGlobalChatEnabled() { return config.getBoolean("global-chat.enabled", true); }
    public String getGlobalChatFormat() { return config.getString("global-chat.format", "&8[G]"); }

    // --- Группы ---
    public String getDefaultFormat() { return config.getString("default-format", "%gchat_format% &7%player_name% &8» &f%message%"); }
    public String getGroupFormat(String group) {
        if (group == null) return null;
        return config.getString("group-formats." + group);
    }

    public Set<String> getGroupFormatKeys() {
        Set<String> keys = new HashSet<>();
        if (config.isConfigurationSection("group-formats")) keys.addAll(config.getConfigurationSection("group-formats").getKeys(false));
        if (config.isConfigurationSection("join-messages")) keys.addAll(config.getConfigurationSection("join-messages").getKeys(false));
        if (config.isConfigurationSection("first-join-messages")) keys.addAll(config.getConfigurationSection("first-join-messages").getKeys(false)); 
        if (config.isConfigurationSection("quit-messages")) keys.addAll(config.getConfigurationSection("quit-messages").getKeys(false));
        return keys;
    }

    // --- ЛС ---
    public boolean isPrivateMessageEnabled() { return config.getBoolean("private-message.enabled", true); }
    public String getPrivateMessageFormatSender() { return config.getString("private-message.format-to-sender"); }
    public String getPrivateMessageFormatReceiver() { return config.getString("private-message.format-to-receiver"); }

    // --- Integrations ---
    public boolean isDiscordEnabled() { return integrationsConfig.getBoolean("discord.enabled", false); }
    public String getDiscordChatFormat() { return integrationsConfig.getString("discord.format"); }
    public boolean isMcToDiscordEnabled() { return integrationsConfig.getBoolean("discord.mc-to-discord", true); }
    public boolean isDiscordToMcEnabled() { return integrationsConfig.getBoolean("discord.discord-to-mc", true); }

    public boolean isTownyEnabled() { return integrationsConfig.getBoolean("towny.enabled", true); }
    public boolean isTownyPreferTag() { return integrationsConfig.getBoolean("towny.prefer-tag", true); }
    public String getTownyFormatTown() { return integrationsConfig.getString("towny.formats.town"); }
    public String getTownyFormatNation() { return integrationsConfig.getString("towny.formats.nation"); }
    public String getTownyFormatBoth() { return integrationsConfig.getString("towny.formats.both"); }

    public boolean isEssentialsMuteEnabled() { return integrationsConfig.getBoolean("mute-hooks.essentials", true); }
    public boolean isAdvancedBanMuteEnabled() { return integrationsConfig.getBoolean("mute-hooks.advancedban", true); }

    // --- Item & Inv Showcase ---
    public boolean isItemShowcaseEnabled() { return config.getBoolean("interactive-chat.item-showcase.enabled", true); }
    public String getItemShowcasePlaceholder() { return config.getString("interactive-chat.item-showcase.placeholder", "[item]"); }
    public String getItemShowcaseFormat() { return config.getString("interactive-chat.item-showcase.format", "&8[&f%amount%x %item_name%&8]"); }
    public String getItemEmptyHandMessage() { return config.getString("interactive-chat.item-showcase.empty-hand-message", "&cУ вас в руке ничего нет!"); }
    public int getItemShowcaseCooldown() { return config.getInt("interactive-chat.item-showcase.cooldown", 10); }

    public boolean isInvShowcaseEnabled() { return config.getBoolean("interactive-chat.inv-showcase.enabled", true); }
    public String getInvShowcasePlaceholder() { return config.getString("interactive-chat.inv-showcase.placeholder", "[inv]"); }
    public String getInvShowcaseFormat() { return config.getString("interactive-chat.inv-showcase.format", "&8[&bИнвентарь %player_name%&8]"); }
    public String getInvHoverHeader() { return config.getString("interactive-chat.inv-showcase.hover-header", "&eИнвентарь:"); }
    public String getInvHoverEmpty() { return config.getString("interactive-chat.inv-showcase.hover-empty", "&7Пусто"); }
    public int getInvShowcaseCooldown() { return config.getInt("interactive-chat.inv-showcase.cooldown", 15); }

    public boolean isEnderShowcaseEnabled() { return config.getBoolean("interactive-chat.ender-showcase.enabled", true); }
    public String getEnderShowcasePlaceholder() { return config.getString("interactive-chat.ender-showcase.placeholder", "[ender]"); }
    public String getEnderShowcaseFormat() { return config.getString("interactive-chat.ender-showcase.format", "&8[&dЭндерчест %player_name%&8]"); }
    public String getEnderHoverHeader() { return config.getString("interactive-chat.ender-showcase.hover-header", "&5Эндерчест:"); }
    public String getEnderHoverEmpty() { return config.getString("interactive-chat.ender-showcase.hover-empty", "&7Пусто"); }
    public int getEnderShowcaseCooldown() { return config.getInt("interactive-chat.ender-showcase.cooldown", 15); }

    // --- Social Spy ---
    public boolean isSocialSpyEnabled() { return config.getBoolean("social-spy.enabled", true); }
    public String getSocialSpyFormat() { return config.getString("social-spy.format", "&8[&cSpy&8] &f%sender% &7-> &f%receiver%&7: &f%message%"); }
    public String getSocialSpyPermission() { return config.getString("social-spy.permission", "gchat.socialspy"); }

    // --- Mentions ---
    public boolean isMentionsEnabled() { return config.getBoolean("player-mentions.enabled", true); }
    public String getMentionPrefix() { return config.getString("player-mentions.prefix", "@"); }
    public String getMentionHighlight() { return config.getString("player-mentions.highlight-format"); }
    public String getMentionSound() { return config.getString("player-mentions.sound"); }
    public float getMentionVolume() { return (float) config.getDouble("player-mentions.sound-volume", 1.0); }
    public float getMentionPitch() { return (float) config.getDouble("player-mentions.sound-pitch", 1.5); }
    public int getMentionCooldown() { return config.getInt("player-mentions.cooldown", 5); }

    // --- Ограничения и Фильтры ---
    public int getMaxMessageLength() { return config.getInt("max-message-length", 256); }
    public boolean isMessageCooldownEnabled() { return config.getBoolean("message-cooldown.enabled", true); }
    public int getMessageCooldownSeconds() { return config.getInt("message-cooldown.seconds", 3); }
    public boolean isBlockDuplicatesEnabled() { return config.getBoolean("message-cooldown.block-duplicates", true); } 
    public int getReloadCooldown() { return config.getInt("reload-cooldown", 5); }

    public boolean isFilterEnabled() { return config.getBoolean("filter.enabled", true); }
    public boolean isFilterBlockMessage() { return config.getBoolean("filter.block-message", false); }
    public List<String> getFilterWords() { return config.getStringList("filter.words"); }
    public boolean isFilterRegexEnabled() { return config.getBoolean("filter.regex-enabled", false); } 
    public List<String> getFilterRegexPatterns() { return config.getStringList("filter.regex-patterns"); } 

    public boolean isBlockCommandsEnabled() { return blockCommandsEnabled; }
    public List<String> getBlockedCommands() { return blockedCommands; }

    public boolean isJoinQuitHandlingEnabled() { return true; }
    public String getJoinMessage(String group) {
        if (group != null && config.contains("join-messages." + group)) return config.getString("join-messages." + group);
        if (config.contains("join-messages.default")) return config.getString("join-messages.default");
        return config.getString("join-message");
    }

    public String getFirstJoinMessage(String group) {
        if (group != null && config.contains("first-join-messages." + group)) return config.getString("first-join-messages." + group);
        if (config.contains("first-join-messages.default")) return config.getString("first-join-messages.default");
        return config.getString("first-join-message"); 
    }

    public String getQuitMessage(String group) {
        if (group != null && config.contains("quit-messages." + group)) return config.getString("quit-messages." + group);
        if (config.contains("quit-messages.default")) return config.getString("quit-messages.default");
        return config.getString("quit-message");
    }

    private String getRandomMessage(String path) {
        if (config.isList(path)) {
            List<String> list = config.getStringList(path);
            if (list.isEmpty()) return null;
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        return config.getString(path);
    }

    public int getChatBubbleLineLength() { return config.getInt("chat-bubbles.line-length", 40); }

    public boolean isDeathMessagesEnabled() { return config.getBoolean("death-messages.enabled", true); }
    public boolean isHideDefaultDeathMessage() { return config.getBoolean("death-messages.hide-default-message", true); }

    public String getDeathMsgPlayerKill() { return getRandomMessage("death-messages.player-kill"); }
    public String getDeathMsgMobKill() { return getRandomMessage("death-messages.mob-kill"); }
    public String getDeathMsgOther() { return getRandomMessage("death-messages.other"); }

    public String getDeathMsgCause(String causeName) {
        return getRandomMessage("death-messages.causes." + causeName);
    }


    public FileConfiguration getBroadcastConfig() { return broadcastConfig; }
    public boolean isBroadcastEnabled() { return broadcastConfig.getBoolean("enabled", false); }
    public int getBroadcastInterval() { return broadcastConfig.getInt("interval", 300); }
    public List<String> getBroadcastMessages() { return broadcastConfig.getStringList("messages"); }
    public String getBroadcastHeader() { return broadcastConfig.getString("header", ""); }
    public String getBroadcastFooter() { return broadcastConfig.getString("footer", ""); }
}
