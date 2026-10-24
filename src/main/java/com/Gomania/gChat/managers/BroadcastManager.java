package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.PlaceholderAPIHook;
import com.Gomania.gChat.utils.ChatColorUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BroadcastManager {

    private final GChat plugin;
    private final ConfigManager config;

    private int broadcastTaskId = -1;
    private int currentBroadcastIndex = 0;
    private final Pattern urlPattern = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^\\)]+)\\)");

    public BroadcastManager(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void scheduleBroadcasts() {
        if (broadcastTaskId != -1) {
            cancelBroadcasts();
        }

        if (config.isBroadcastEnabled()) {
            List<String> messages = config.getBroadcastMessages();
            if (messages.isEmpty()) {
                plugin.getLogger().warning("Broadcast is enabled, but no messages are configured.");
                return;
            }

            int interval = config.getBroadcastInterval();
            if (interval <= 0) return;

            broadcastTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (messages.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) return;
                String message = messages.get(currentBroadcastIndex);
                sendFormattedBroadcast(message);
                currentBroadcastIndex = (currentBroadcastIndex + 1) % messages.size();
            }, interval * 20L, interval * 20L);

            plugin.getLogger().info("Broadcast system started with interval: " + interval + " seconds");
        }
    }

    public void cancelBroadcasts() {
        if (broadcastTaskId != -1) {
            Bukkit.getScheduler().cancelTask(broadcastTaskId);
            broadcastTaskId = -1;
        }
    }

    public void reload() {
        cancelBroadcasts();
        scheduleBroadcasts();
    }

    public void sendNextBroadcastMessage() {
        List<String> messages = config.getBroadcastMessages();
        if (messages.isEmpty()) return;
        String message = messages.get(currentBroadcastIndex);
        sendFormattedBroadcast(message);
        currentBroadcastIndex = (currentBroadcastIndex + 1) % messages.size();
    }

    public void sendFormattedBroadcast(String message) {
        if (message == null || message.isEmpty()) return;
        message = message.replace("%nl%", "\n").replace("\\n", "\n");

        String header = config.getBroadcastHeader();
        String footer = config.getBroadcastFooter();

        // Отправляем каждому игроку индивидуально
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (header != null && !header.isEmpty()) {
                processAndSend(player, header);
            }
            processAndSend(player, message);
            if (footer != null && !footer.isEmpty()) {
                processAndSend(player, footer);
            }
        }

        // Лог в консоль (чистый текст)
        plugin.getLogger().info("[BROADCAST] " + ChatColorUtils.stripColors(message));
    }

    /**
     * Обрабатывает сообщение для конкретного игрока (PAPI + Colors + Links)
     */
    private void processAndSend(Player player, String rawMessage) {
        // 1. Применяем цвета
        String text = ChatColorUtils.translateColors(rawMessage, true, true);

        // 2. Применяем PAPI (если есть)
        PlaceholderAPIHook papiHook = plugin.getPlaceholderAPIHook();
        if (papiHook != null) {
            text = papiHook.setPlaceholders(player, text);
        }

        // 3. Ищем ссылки и превращаем в компоненты
        TextComponent component = buildComponentWithLinks(text);

        // 4. Отправляем
        player.spigot().sendMessage(component);
    }

    private TextComponent buildComponentWithLinks(String message) {
        Matcher matcher = urlPattern.matcher(message);
        TextComponent finalComponent = new TextComponent("");
        int lastEnd = 0;

        while (matcher.find()) {
            // Добавляем текст до ссылки
            if (matcher.start() > lastEnd) {
                String before = message.substring(lastEnd, matcher.start());
                finalComponent.addExtra(new TextComponent(TextComponent.fromLegacyText(before)));
            }

            // Группа 1: Текст, который будет отображаться
            // Группа 2: Сама ссылка
            String displayText = matcher.group(1);
            String url = matcher.group(2);

            TextComponent urlComponent = new TextComponent(TextComponent.fromLegacyText(displayText));
            urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

            String hoverText = plugin.getMessageManager().getMessage("click-to-open-url").replace("%url%", url);
            urlComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

            finalComponent.addExtra(urlComponent);
            lastEnd = matcher.end();
        }

        // Добавляем оставшийся хвост сообщения
        if (lastEnd < message.length()) {
            finalComponent.addExtra(new TextComponent(TextComponent.fromLegacyText(message.substring(lastEnd))));
        }
        return finalComponent;
    }
}