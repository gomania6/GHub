package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.DiscordSRVHook;
import com.Gomania.gChat.hooks.LuckPermsHook;
import com.Gomania.gChat.hooks.PlaceholderAPIHook;
import com.Gomania.gChat.managers.PlayerSettingsManager.PlayerSettings;
import com.Gomania.gChat.utils.ChatColorUtils;
import com.Gomania.gChat.utils.ItemUtils;
import com.Gomania.gChat.utils.SoundUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager {
    private boolean isChatGlobalMuted = false;
    private final GChat plugin;
    private final ConfigManager config;
    private final MessageManager messages;

    private final CooldownManager messageCooldown = new CooldownManager();
    private final CooldownManager mentionCooldown = new CooldownManager();

    // Кулдауны интерактивных тегов
    private final CooldownManager itemCooldown = new CooldownManager();
    private final CooldownManager invCooldown = new CooldownManager();
    private final CooldownManager enderCooldown = new CooldownManager();

    private final Map<UUID, UUID> replyTarget = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();

    private Pattern mentionPattern;
    private Pattern urlPattern;
    private final Pattern ipPattern = Pattern.compile("(?i)((?<![0-9])(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(?![0-9]))");

    public ChatManager(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessageManager();
        reload();
    }

    public void reload() {
        mentionPattern = Pattern.compile(Pattern.quote(config.getMentionPrefix()) + "([A-Za-z0-9_]{3,16})");
        urlPattern = Pattern.compile("(?:(https?)://)?([-\\w_.]+\\.[a-z]{2,})(/\\S*)?", Pattern.CASE_INSENSITIVE);
        messageCooldown.clear();
        mentionCooldown.clear();
        itemCooldown.clear();
        invCooldown.clear();
        enderCooldown.clear();
        replyTarget.clear();
        lastMessages.clear();
    }

    public void cleanUpPlayer(UUID uuid) {
        replyTarget.remove(uuid);
        replyTarget.values().removeIf(targetId -> targetId.equals(uuid));
        messageCooldown.remove(uuid);
        mentionCooldown.remove(uuid);
        itemCooldown.remove(uuid);
        invCooldown.remove(uuid);
        enderCooldown.remove(uuid);
        lastMessages.remove(uuid);
    }

    // ==========================================
    // ЛИЧНЫЕ СООБЩЕНИЯ (PM) & SOCIAL SPY
    // ==========================================

    public void handlePrivateMessage(Player sender, Player target, String message) {
        if (!sender.hasPermission("gchat.chat.msg")) {
            sender.sendMessage(messages.getMessage("no-permission"));
            return;
        }

        if (target == null || !target.isOnline()) {
            sender.sendMessage(messages.getMessage("player-not-found"));
            return;
        }

        if (plugin.getIgnoreManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(messages.getMessage("pm-ignored-target"));
            return;
        }
        if (plugin.getIgnoreManager().isIgnored(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(messages.getMessage("pm-ignored-self"));
            return;
        }

        if (plugin.getMuteManager().isPlayerMuted(sender)) {
            sender.sendMessage(messages.getMessage("muted-message"));
            return;
        }

        replyTarget.put(sender.getUniqueId(), target.getUniqueId());
        replyTarget.put(target.getUniqueId(), sender.getUniqueId());

        String formatSender = config.getPrivateMessageFormatSender()
                .replace("%sender%", sender.getName())
                .replace("%receiver%", target.getName())
                .replace("%message%", message);

        String formatReceiver = config.getPrivateMessageFormatReceiver()
                .replace("%sender%", sender.getName())
                .replace("%receiver%", target.getName())
                .replace("%message%", message);

        String finalSenderMsg = ChatColorUtils.translateColors(formatSender, true, true, true);
        TextComponent senderComponent = new TextComponent(TextComponent.fromLegacyText(finalSenderMsg));
        senderComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + target.getName() + " "));
        String hoverWrite = messages.getMessage("click-to-write").replace("%player%", target.getName());
        senderComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverWrite)));

        String finalReceiverMsg = ChatColorUtils.translateColors(formatReceiver, true, true, true);
        TextComponent receiverComponent = new TextComponent(TextComponent.fromLegacyText(finalReceiverMsg));
        receiverComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + sender.getName() + " "));
        String hoverReply = messages.getMessage("click-to-reply").replace("%player%", sender.getName());
        receiverComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverReply)));

        sender.spigot().sendMessage(senderComponent);
        target.spigot().sendMessage(receiverComponent);

        if (config.isSocialSpyEnabled()) {
            String spyFormat = config.getSocialSpyFormat()
                    .replace("%sender%", sender.getName())
                    .replace("%receiver%", target.getName())
                    .replace("%message%", message);
            String finalSpyMsg = ChatColorUtils.translateColors(spyFormat, true, true, true);
            String spyPerm = config.getSocialSpyPermission();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(spyPerm) && !p.getUniqueId().equals(sender.getUniqueId()) && !p.getUniqueId().equals(target.getUniqueId())) {
                    p.sendMessage(finalSpyMsg);
                }
            }
        }

        if (plugin.getConfig().getBoolean("private-message.sound-enabled", true)) {
            String soundName = plugin.getConfig().getString("private-message.sound");
            Sound sound = SoundUtils.getSound(soundName);
            if (sound != null) {
                target.playSound(target.getLocation(), sound, 1.0f, 1.0f);
            }
        }

        if (plugin.getConfig().getBoolean("private-message.action-bar.enabled", true)) {
            String abFormat = plugin.getConfig().getString("private-message.action-bar.format", "&aВам пишет %sender%");
            String abText = abFormat.replace("%sender%", sender.getName());
            target.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColorUtils.translateColors(abText, true, true, true)));
        }

        if (plugin.getLogManager() != null) {
            plugin.getLogManager().log("[MSG] " + sender.getName() + " -> " + target.getName() + ": " + message);
        }
    }

    public void handleReply(Player sender, String message) {
        if (!sender.hasPermission("gchat.chat.reply")) {
            sender.sendMessage(messages.getMessage("no-permission"));
            return;
        }

        UUID targetUUID = replyTarget.get(sender.getUniqueId());
        if (targetUUID == null) {
            sender.sendMessage(messages.getMessage("pm-nobody-to-reply"));
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(messages.getMessage("player-not-found"));
            return;
        }

        handlePrivateMessage(sender, target, message);
    }

    // ==========================================
    // ОБРАБОТКА ЧАТА (ОСНОВНОЙ МЕТОД)
    // ==========================================

    public void processChatMessage(Player player, String originalMessage, String chatMode) {
        processChatMessage(player, originalMessage, chatMode, null);
    }

    public void processChatMessage(Player player, String originalMessage, String chatMode, ItemStack showcasedItem) {
        // Гарантируем выполнение в основном потоке сервера
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> processChatMessage(player, originalMessage, chatMode, showcasedItem));
            return;
        }

        if (plugin.getMuteManager().isPlayerMuted(player)) {
            player.sendMessage(messages.getMessage("muted-message"));
            return;
        }

        if (!hasChatPermission(player, chatMode)) return;

        if (isChatGlobalMuted && !player.hasPermission("gchat.mutechat.bypass")) {
            player.sendMessage(messages.getMessage("chat-is-muted"));
            return;
        }

        if (!checkCooldown(player)) return;

        if (config.isBlockDuplicatesEnabled() && !player.hasPermission("gchat.bypass.cooldown")) {
            String lastMsg = lastMessages.get(player.getUniqueId());
            if (lastMsg != null && lastMsg.equalsIgnoreCase(originalMessage)) {
                player.sendMessage(messages.getMessage("message-duplicate"));
                return;
            }
        }
        lastMessages.put(player.getUniqueId(), originalMessage);

        PlayerSettings settings = plugin.getSettingsManager().getSettings(player.getUniqueId());

        if (chatMode.equalsIgnoreCase("global") && !settings.globalChatEnabled) {
            player.sendMessage(messages.getMessage("global-chat-disabled-self"));
            return;
        }

        String processedMessage = filterModeration(player, originalMessage);
        if (processedMessage == null) return;

        processedMessage = filterBadWords(player, processedMessage);
        if (processedMessage == null) return;

        processedMessage = filterLinks(player, processedMessage);
        if (processedMessage == null) return;

        if (!checkMessageLength(player, processedMessage)) return;

        String messageWithMentions = processedMessage;
        Set<Player> mentionedPlayers = new HashSet<>();
        boolean everyonePing = false;

        if (config.isMentionsEnabled()) {
            String everyoneTrigger = plugin.getConfig().getString("player-mentions.everyone.trigger", "@everyone");
            boolean everyoneEnabled = plugin.getConfig().getBoolean("player-mentions.everyone.enabled", true);

            if (everyoneEnabled && Pattern.compile(Pattern.quote(everyoneTrigger), Pattern.CASE_INSENSITIVE).matcher(processedMessage).find()) {
                if (player.hasPermission("gchat.chat.everyone")) {
                    messageWithMentions = processEveryoneMention(player, messageWithMentions, everyoneTrigger);
                    everyonePing = true;
                }
            }
            if (player.hasPermission("gchat.chat.ping")) {
                messageWithMentions = processPlayerMentions(player, messageWithMentions, mentionedPlayers);
            }
        }

        // ==========================================
        // ЗАЩИТА ИНТЕРАКТИВНЫХ ТЕГОВ ОТ ГРАДИЕНТОВ
        // ==========================================
        String itemPh = config.getItemShowcasePlaceholder();
        String invPh = config.getInvShowcasePlaceholder();
        String enderPh = config.getEnderShowcasePlaceholder();

        ItemStack actualShowcasedItem = showcasedItem;
        if (config.isItemShowcaseEnabled() && messageWithMentions.contains(itemPh)) {
            if (actualShowcasedItem == null) {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand != null && inHand.getType() != Material.AIR) {
                    actualShowcasedItem = inHand.clone();
                }
            }

            if (actualShowcasedItem == null || actualShowcasedItem.getType() == Material.AIR) {
                String emptyHandMsg = config.getItemEmptyHandMessage();
                player.sendMessage(ChatColorUtils.translateColors(emptyHandMsg, true, true, true));
                return;
            }

            if (itemCooldown.check(player.getUniqueId(), config.getItemShowcaseCooldown())) {
                messageWithMentions = messageWithMentions.replace(itemPh, "\uE000");
            } else {
                long left = itemCooldown.getRemaining(player.getUniqueId(), config.getItemShowcaseCooldown());
                player.sendMessage(messages.getMessage("tag-cooldown-item").replace("{time}", String.valueOf(left)));
                messageWithMentions = messageWithMentions.replace(itemPh, "");
            }
        }

        if (config.isInvShowcaseEnabled() && messageWithMentions.contains(invPh)) {
            if (invCooldown.check(player.getUniqueId(), config.getInvShowcaseCooldown())) {
                messageWithMentions = messageWithMentions.replace(invPh, "\uE001");
            } else {
                long left = invCooldown.getRemaining(player.getUniqueId(), config.getInvShowcaseCooldown());
                player.sendMessage(messages.getMessage("tag-cooldown-inv").replace("{time}", String.valueOf(left)));
                messageWithMentions = messageWithMentions.replace(invPh, "");
            }
        }

        if (config.isEnderShowcaseEnabled() && messageWithMentions.contains(enderPh)) {
            if (enderCooldown.check(player.getUniqueId(), config.getEnderShowcaseCooldown())) {
                messageWithMentions = messageWithMentions.replace(enderPh, "\uE002");
            } else {
                long left = enderCooldown.getRemaining(player.getUniqueId(), config.getEnderShowcaseCooldown());
                player.sendMessage(messages.getMessage("tag-cooldown-ender").replace("{time}", String.valueOf(left)));
                messageWithMentions = messageWithMentions.replace(enderPh, "");
            }
        }

        String standardMessageContent = ChatColorUtils.translateColors(
                messageWithMentions, canUseHex(player), canUseStandard(player), canUseGradients(player)
        );
        String rawFormat = getRawFormat(player, chatMode);
        String standardFormat = prepareFormat(player, rawFormat);

        TextComponent standardInteractiveMessage = buildInteractiveMessage(player, standardFormat, standardMessageContent, actualShowcasedItem);
        TextComponent themedInteractiveMessage = standardInteractiveMessage;

        if (!settings.chatColorTheme.isEmpty()) {
            String translatedTheme = ChatColorUtils.translateColors(settings.chatColorTheme, true, true, true);
            String themedFormat = standardFormat;
            String themedMessageContent;

            if (settings.chatColorMode.equalsIgnoreCase("full")) {
                themedFormat = translatedTheme + ChatColorUtils.stripColors(themedFormat);
                themedMessageContent = translatedTheme + standardMessageContent;
            } else {
                themedMessageContent = translatedTheme + standardMessageContent;
            }

            themedInteractiveMessage = buildInteractiveMessage(player, themedFormat, themedMessageContent, actualShowcasedItem);
        }

        String rawForLog = standardMessageContent
                .replace("\uE000", itemPh)
                .replace("\uE001", invPh)
                .replace("\uE002", enderPh);

        if (chatMode.equalsIgnoreCase("local")) {
            sendLocalMessage(player, standardInteractiveMessage, themedInteractiveMessage);
        } else {
            sendGlobalMessage(player, standardInteractiveMessage, themedInteractiveMessage);

            if (plugin.getDependencyManager().hasDiscordSRV()) {
                DiscordSRVHook discordHook = plugin.getDiscordSRVHook();
                if (discordHook != null) discordHook.sendToDiscord(player, rawForLog);
            }
        }

        if (config.isMentionsEnabled()) {
            if (everyonePing) {
                notifyEveryone(player);
            } else if (!mentionedPlayers.isEmpty()) {
                notifyMentionedPlayers(player, mentionedPlayers);
            }
        }

        if (plugin.getChatBubbleManager() != null) {
            plugin.getChatBubbleManager().displayBubble(player, rawForLog);
        }

        if (plugin.getLogManager() != null) {
            plugin.getLogManager().log("[" + chatMode.toUpperCase() + "] " + player.getName() + ": " + originalMessage);
        }

        if (config.isLogToConsoleEnabled()) {
            Bukkit.getLogger().info("[" + chatMode.toUpperCase() + "] " + player.getName() + ": " + ChatColorUtils.stripColors(rawForLog));
        }
    }

    // ==========================================
    // ОТПРАВКА СООБЩЕНИЙ
    // ==========================================

    public void sendLocalMessage(Player sender, TextComponent standardMessage, TextComponent themedMessage) {
        if (!config.isLocalChatEnabled()) {
            if (config.showDisabledLocalChatMessage()) sender.sendMessage(messages.getMessage("local-chat-disabled"));
            return;
        }
        int radius = config.getLocalChatRadius();
        double radiusSquared = radius * radius;
        int recipients = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (plugin.getIgnoreManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) continue;

            if (target.getWorld().equals(sender.getWorld()) && target.getLocation().distanceSquared(sender.getLocation()) <= radiusSquared) {
                if (target.equals(sender)) {
                    target.spigot().sendMessage(themedMessage);
                } else {
                    target.spigot().sendMessage(standardMessage);
                }
                if (!target.getUniqueId().equals(sender.getUniqueId())) recipients++;
            }
        }
        if (recipients == 0 && config.showLocalChatSilentMessage()) sender.sendMessage(messages.getMessage("local-chat-silent"));
    }

    public void sendGlobalMessage(TextComponent message) {
        if (!config.isGlobalChatEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getSettingsManager().getSettings(player.getUniqueId()).globalChatEnabled) continue;
            player.spigot().sendMessage(message);
        }
    }

    public void sendGlobalMessage(Player sender, TextComponent standardMessage, TextComponent themedMessage) {
        if (!config.isGlobalChatEnabled()) return;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (plugin.getIgnoreManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) continue;
            if (!plugin.getSettingsManager().getSettings(target.getUniqueId()).globalChatEnabled) continue;

            if (target.equals(sender)) {
                target.spigot().sendMessage(themedMessage);
            } else {
                target.spigot().sendMessage(standardMessage);
            }
        }
    }

    // ==========================================
    // СБОРКА И ФИЛЬТРАЦИЯ
    // ==========================================

    private TextComponent buildInteractiveMessage(Player player, String format, String messageContent, ItemStack showcasedItem) {
        Pattern msgGradientPattern = Pattern.compile("\\{#([0-9A-Fa-f]{6})>\\}%message%\\{#([0-9A-Fa-f]{6})<\\}");
        Matcher matcher = msgGradientPattern.matcher(format);

        if (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);

            if (messageContent != null && !messageContent.isEmpty()) {
                messageContent = ChatColorUtils.translateColors(
                        "{#" + startHex + ">}" + messageContent + "{#" + endHex + "<}",
                        false, false, true
                );
            }
            format = matcher.replaceFirst("%message%");
        }

        TextComponent root = new TextComponent("");
        boolean interactiveEnabled = plugin.getConfig().getBoolean("interactive-chat.enabled", true);

        String replacedFormat = format.replace("%player_name%", player.getName());
        String coloredFormat = ChatColorUtils.translateColors(replacedFormat, true, true, true);
        String[] parts = coloredFormat.split("%message%", -1);

        if (parts.length > 0 && !parts[0].isEmpty()) {
            BaseComponent[] prefixComponents = TextComponent.fromLegacyText(parts[0]);
            if (interactiveEnabled) applyInteractivity(player, prefixComponents);
            for (BaseComponent bc : prefixComponents) root.addExtra(bc);
        }

        if (messageContent != null && !messageContent.isEmpty()) {
            String lastColors = "";
            if (parts.length > 0 && !parts[0].isEmpty()) {
                lastColors = extractLastColors(parts[0]);
            }

            String finalContent = lastColors + messageContent;
            appendMessageWithItemSupport(root, player, finalContent, showcasedItem);
        }

        if (parts.length > 1 && !parts[1].isEmpty()) {
            BaseComponent[] suffixComponents = TextComponent.fromLegacyText(parts[1]);
            for (BaseComponent bc : suffixComponents) root.addExtra(bc);
        }

        return root;
    }

    private String extractLastColors(String input) {
        StringBuilder result = new StringBuilder();
        int length = input.length();

        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == '§' && index < length - 1) {
                char c = input.charAt(index + 1);
                if (index >= 12 && input.charAt(index - 11) == '§' && (input.charAt(index - 10) == 'x' || input.charAt(index - 10) == 'X')) {
                    return input.substring(index - 11, index + 2) + result.toString();
                }
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == 'r' || c == 'R') {
                    result.insert(0, "§" + c);
                    break;
                } else if ((c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O')) {
                    result.insert(0, "§" + c);
                }
            }
        }
        return result.toString();
    }

    // ==========================================
    // ИНТЕРАКТИВНЫЕ ТЕГИ ([item], [inv], [ender])
    // ==========================================

    private void appendMessageWithItemSupport(TextComponent root, Player player, String message, ItemStack showcasedItem) {
        List<BaseComponent> parsedComponents = parseTags(player, message, showcasedItem);
        for (BaseComponent c : parsedComponents) {
            root.addExtra(c);
        }
    }

    private List<BaseComponent> parseTags(Player player, String text, ItemStack showcasedItem) {
        List<BaseComponent> components = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        String lastColors = "";

        String itemNoneText = messages.getMessage("item-none");
        String itemEnchantedText = messages.getMessage("item-enchanted");

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\uE000' || c == '\uE001' || c == '\uE002') {
                if (currentText.length() > 0) {
                    String part = currentText.toString();
                    components.addAll(Arrays.asList(TextComponent.fromLegacyText(lastColors + part)));
                    lastColors = extractLastColors(lastColors + part);
                    currentText.setLength(0);
                }

                if (c == '\uE000') {
                    components.add(ItemUtils.convertItemToComponent(showcasedItem, config.getItemShowcaseFormat(), itemNoneText, itemEnchantedText));
                } else if (c == '\uE001') {
                    components.add(ItemUtils.convertInventoryToComponent(player, player.getInventory().getContents(),
                            config.getInvShowcaseFormat(), config.getInvHoverHeader(), config.getInvHoverEmpty()));
                } else if (c == '\uE002') {
                    components.add(ItemUtils.convertInventoryToComponent(player, player.getEnderChest().getContents(),
                            config.getEnderShowcaseFormat(), config.getEnderHoverHeader(), config.getEnderHoverEmpty()));
                }
            } else {
                currentText.append(c);
            }
        }

        if (currentText.length() > 0) {
            components.addAll(Arrays.asList(TextComponent.fromLegacyText(lastColors + currentText.toString())));
        }

        return components;
    }

    private void applyInteractivity(Player player, BaseComponent[] components) {
        FileConfiguration cfg = plugin.getConfig();
        PlaceholderAPIHook papi = plugin.getPlaceholderAPIHook();

        HoverEvent hoverEvent = null;
        ClickEvent clickEvent = null;

        if (cfg.getBoolean("interactive-chat.hover.enabled")) {
            List<String> lines = cfg.getStringList("interactive-chat.hover.text");
            StringBuilder hoverText = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).replace("%player_name%", player.getName());

                if (plugin.getLuckPermsHook() != null) {
                    String prefix = plugin.getLuckPermsHook().getPlayerPrefix(player.getUniqueId());
                    String group = plugin.getLuckPermsHook().getPlayerGroup(player.getUniqueId(), config.getGroupFormatKeys());
                    line = line.replace("%luckperms_prefix%", prefix != null ? prefix : "")
                            .replace("%luckperms_group%", group != null ? group : "default");
                } else {
                    line = line.replace("%luckperms_prefix%", "")
                            .replace("%luckperms_group%", "default");
                }

                if (papi != null) {
                    line = papi.setPlaceholders(player, line);
                }

                hoverText.append(ChatColorUtils.translateColors(line, true, true, true));
                if (i < lines.size() - 1) hoverText.append("\n");
            }
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString()));
        }

        if (cfg.getBoolean("interactive-chat.click.enabled")) {
            String actionStr = cfg.getString("interactive-chat.click.action", "SUGGEST_COMMAND");
            String value = cfg.getString("interactive-chat.click.value", "/msg %player_name% ").replace("%player_name%", player.getName());
            try {
                clickEvent = new ClickEvent(ClickEvent.Action.valueOf(actionStr), value);
            } catch (Exception e) {
                clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value);
            }
        }

        for (BaseComponent bc : components) {
            if (hoverEvent != null) bc.setHoverEvent(hoverEvent);
            if (clickEvent != null) bc.setClickEvent(clickEvent);
        }
    }

    private String getRawFormat(Player player, String chatMode) {
        String baseFormat = chatMode.equals("local") ? config.getLocalChatFormat() : config.getGlobalChatFormat();
        LuckPermsHook lpHook = plugin.getLuckPermsHook();

        String playerGroup = (lpHook != null) ? lpHook.getPlayerGroup(player.getUniqueId(), config.getGroupFormatKeys()) : null;
        String groupFormat = config.getGroupFormat(playerGroup);
        String finalFormat = (groupFormat != null) ? groupFormat : config.getDefaultFormat();

        return finalFormat.replace("%gchat_format%", baseFormat != null ? baseFormat : "");
    }

    private String prepareFormat(Player player, String format) {
        LuckPermsHook lpHook = plugin.getLuckPermsHook();
        PlaceholderAPIHook papiHook = plugin.getPlaceholderAPIHook();

        format = format.replace("%player_name%", "%%temp_player_name%%");

        if (papiHook != null) {
            format = papiHook.setPlaceholders(player, format);
        }
        format = format.replace("%%temp_player_name%%", "%player_name%");
        format = format.replace("%player_world%", player.getWorld().getName());

        if (format.contains("%luckperms_prefix%")) {
            String prefix = (lpHook != null) ? lpHook.getPlayerPrefix(player.getUniqueId()) : "";
            if (prefix != null && !prefix.isEmpty()) {
                format = format.replace("%luckperms_prefix%", prefix + "&r");
            } else {
                format = format.replace("%luckperms_prefix%", "");
            }
        }

        if (format.contains("%luckperms_suffix%")) {
            String suffix = (lpHook != null) ? lpHook.getPlayerSuffix(player.getUniqueId()) : "";
            if (suffix != null && !suffix.isEmpty()) {
                format = format.replace("%luckperms_suffix%", suffix + "&r");
            } else {
                format = format.replace("%luckperms_suffix%", "");
            }
        }

        return format;
    }

    private String filterModeration(Player player, String message) {
        boolean bypass = player.hasPermission("gchat.bypass.filter");
        if (bypass) return message;

        if (config.isModBlockIps() && ipPattern.matcher(message).find()) {
            if (!player.hasPermission("gchat.link.bypass")) {
                player.sendMessage(messages.getMessage("ip-blocked"));
                return null;
            }
        }

        if (config.isAntiCapsEnabled() && !player.hasPermission("gchat.bypass.caps")) {
            if (message.length() >= config.getAntiCapsMinLength()) {
                int upperCount = 0;
                int lettersCount = 0;
                for (char c : message.toCharArray()) {
                    if (Character.isLetter(c)) {
                        lettersCount++;
                        if (Character.isUpperCase(c)) upperCount++;
                    }
                }
                if (lettersCount > 0) {
                    double percentage = (double) upperCount / lettersCount * 100;
                    if (percentage > config.getAntiCapsMaxPercentage()) {
                        return message.toLowerCase();
                    }
                }
            }
        }
        return message;
    }

    private String filterLinks(Player player, String message) {
        if (!config.isModBlockLinks() || player.hasPermission("gchat.link.bypass")) return message;

        if (urlPattern.matcher(message).find()) {
            player.sendMessage(messages.getMessage("illegal-link"));
            sendAlert(player, messages.getMessage("alert-reason-link"), message);
            return null;
        }
        return message;
    }

    private String processEveryoneMention(Player sender, String message, String trigger) {
        String format = plugin.getConfig().getString("player-mentions.everyone.format", "&c&l@everyone&r");
        return message.replaceAll("(?i)" + Pattern.quote(trigger), ChatColorUtils.translateColors(format, true, true, true));
    }

    private String processPlayerMentions(Player sender, String message, Set<Player> mentionedPlayers) {
        Matcher matcher = mentionPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        int mentionCd = config.getMentionCooldown();
        boolean canMention = mentionCooldown.check(sender.getUniqueId(), mentionCd);
        boolean triedToMention = false;

        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            Player mentionedPlayer = Bukkit.getPlayerExact(mentionedName);
            if (mentionedPlayer != null && mentionedPlayer.isOnline() && !mentionedPlayer.equals(sender)) {
                triedToMention = true;
                if (canMention) {
                    String highlight = ChatColorUtils.translateColors(
                            config.getMentionHighlight().replace("%player%", mentionedName), true, true, true
                    );
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(highlight));
                    mentionedPlayers.add(mentionedPlayer);
                } else {
                    matcher.appendReplacement(sb, matcher.group(0));
                }
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        if (!canMention && triedToMention) {
            long remaining = mentionCooldown.getRemaining(sender.getUniqueId(), mentionCd);
            sender.sendMessage(messages.getMessage("mention-cooldown").replace("{time}", String.valueOf(remaining)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void notifyEveryone(Player sender) {
        String soundName = plugin.getConfig().getString("player-mentions.everyone.sound", "BLOCK_NOTE_BLOCK_PLING");
        String actionBarFormat = plugin.getConfig().getString("player-mentions.everyone.action-bar-text", "&cВнимание! Общее упоминание от %player%");
        boolean soundEnabled = plugin.getConfig().getBoolean("player-mentions.everyone.sound-enabled", true);
        Sound sound = soundEnabled ? SoundUtils.getSound(soundName) : null;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(sender)) continue;
            if (sound != null) p.playSound(p.getLocation(), sound, 1f, 1f);
            String abText = actionBarFormat.replace("%player%", sender.getName());
            abText = ChatColorUtils.translateColors(abText, true, true, true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(abText));
        }
    }

    private void sendAlert(Player sender, String reason, String originalMessage) {
        String alertMsg = messages.getMessage("alert-format")
                .replace("%player%", sender.getName())
                .replace("%reason%", reason)
                .replace("%message%", originalMessage);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("gchat.alerts")) {
                p.sendMessage(alertMsg);
            }
        }
    }

    private void notifyMentionedPlayers(Player sender, Set<Player> mentionedPlayers) {
        float volume = config.getMentionVolume();
        float pitch = config.getMentionPitch();
        String abFormat = plugin.getConfig().getString("player-mentions.action-bar.text", "&eВас упомянул %player%");

        for (Player p : mentionedPlayers) {
            PlayerSettings pSettings = plugin.getSettingsManager().getSettings(p.getUniqueId());
            String soundType = pSettings.mentionSound;
            Sound sound = null;

            if (!soundType.equalsIgnoreCase("NONE")) {
                if (soundType.equalsIgnoreCase("DEFAULT")) {
                    if (plugin.getConfig().getBoolean("player-mentions.sound-enabled", true)) {
                        sound = SoundUtils.getSound(config.getMentionSound());
                    }
                } else {
                    if (soundType.equalsIgnoreCase("XP")) sound = SoundUtils.getSound("ENTITY_EXPERIENCE_ORB_PICKUP");
                    else if (soundType.equalsIgnoreCase("PLING")) sound = SoundUtils.getSound("BLOCK_NOTE_BLOCK_PLING");
                    else if (soundType.equalsIgnoreCase("ANVIL")) sound = SoundUtils.getSound("BLOCK_ANVIL_LAND");
                }
            }

            if (sound != null) p.playSound(p.getLocation(), sound, volume, pitch);
            String abText = abFormat.replace("%player%", sender.getName());
            abText = ChatColorUtils.translateColors(abText, true, true, true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(abText));
        }
    }

    private boolean checkCooldown(Player player) {
        if (!config.isMessageCooldownEnabled() || player.hasPermission("gchat.bypass.cooldown")) return true;
        int cd = config.getMessageCooldownSeconds();
        if (!messageCooldown.check(player.getUniqueId(), cd)) {
            player.sendMessage(messages.getMessage("message-cooldown").replace("{time}", String.valueOf(messageCooldown.getRemaining(player.getUniqueId(), cd))));
            return false;
        }
        return true;
    }

    private String filterBadWords(Player player, String message) {
        if (!config.isFilterEnabled() || player.hasPermission("gchat.bypass.filter")) return message;

        boolean isBlocked = false;
        String originalMsg = message;

        for (String word : config.getFilterWords()) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                isBlocked = true;
                message = message.replaceAll("(?i)" + Pattern.quote(word), "***");
            }
        }

        if (config.isFilterRegexEnabled()) {
            for (String patternStr : config.getFilterRegexPatterns()) {
                try {
                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        isBlocked = true;
                        message = matcher.replaceAll("***");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка в регулярном выражении фильтра: " + patternStr);
                }
            }
        }

        if (isBlocked && config.isFilterBlockMessage()) {
            player.sendMessage(messages.getMessage("message-blocked"));
            sendAlert(player, messages.getMessage("alert-reason-badword"), originalMsg);
            return null;
        }

        return message;
    }

    private boolean checkMessageLength(Player player, String message) {
        if (message.length() > config.getMaxMessageLength() && !player.hasPermission("gchat.bypass.length")) {
            player.sendMessage(messages.getMessage("message-too-long").replace("{max}", String.valueOf(config.getMaxMessageLength())));
            return false;
        }
        return true;
    }

    private boolean hasChatPermission(Player player, String chatType) {
        if (player.hasPermission("gchat.bypass")) return true;
        return chatType.equalsIgnoreCase("global") || chatType.equalsIgnoreCase("local");
    }

    private boolean canUseStandard(Player player) { return player.hasPermission("gchat.color.codes"); }
    private boolean canUseHex(Player player) { return player.hasPermission("gchat.color.hex"); }
    private boolean canUseGradients(Player player) { return player.hasPermission("gchat.gradient"); }

    public boolean toggleChatMute() {
        isChatGlobalMuted = !isChatGlobalMuted;
        return isChatGlobalMuted;
    }
}