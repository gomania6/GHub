package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatBubbleManager implements Listener {

    private final GChat plugin;
    private final ConfigManager config;
    private final Map<UUID, BubbleInstance> activeBubbles = new ConcurrentHashMap<>();

    public ChatBubbleManager(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void displayBubble(Player player, String message) {
        if (!config.isChatBubblesEnabled() || player == null || !player.isOnline()) return;

        // Гарантируем выполнение в основном потоке
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> displayBubble(player, message));
            return;
        }

        // Удаляем предыдущий бабл игрока, если он ещё висит
        removeBubble(player.getUniqueId());

        String text = message;
        if (config.isChatBubbleStripColors()) {
            text = ChatColorUtils.stripColors(text);
        }

        String format = config.getChatBubbleFormat().replace("%message%", text);
        String coloredText = ChatColorUtils.translateColors(format, true, true, true);

        int lineLength = Math.max(10, config.getChatBubbleLineLength());
        String wrappedText = wrapText(coloredText, lineLength);

        double heightOffset = config.getChatBubbleHeight();
        int displaySeconds = Math.max(1, config.getChatBubbleTime());

        Location spawnLoc = player.getEyeLocation();
        TextDisplay display = player.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
            entity.setText(wrappedText);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            entity.setPersistent(false);
            entity.setInvulnerable(true);

            // Настраиваем ширину в пикселях пропорционально line-length (в среднем ~6 пикселей на символ)
            entity.setLineWidth(Math.max(120, lineLength * 6));

            // Смещение по высоте относительно точки крепления пассажира (головы игрока)
            float yTranslation = (float) Math.max(0.2, heightOffset - 1.8);
            Transformation transformation = new Transformation(
                    new Vector3f(0f, yTranslation, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            );
            entity.setTransformation(transformation);
        });

        // Привязываем бабл к игроку как пассажира (двигается за игроком на уровне клиента без нагрузки на TPS)
        player.addPassenger(display);

        BukkitTask removalTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player.getUniqueId());
        }, displaySeconds * 20L);

        activeBubbles.put(player.getUniqueId(), new BubbleInstance(display, removalTask));
    }

    /**
     * Переносит длинный текст по строкам в соответствии с line-length, сохраняя цветовые коды.
     */
    private String wrapText(String text, int maxLineLength) {
        if (text == null || text.isEmpty() || maxLineLength <= 0) return "";

        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        int currentVisibleLength = 0;
        String lastColors = "";

        for (String word : words) {
            String strippedWord = ChatColorUtils.stripColors(word);
            int wordLength = strippedWord != null ? strippedWord.length() : word.length();

            if (currentVisibleLength + wordLength + (currentVisibleLength > 0 ? 1 : 0) > maxLineLength) {
                if (currentLine.length() > 0) {
                    String builtLine = currentLine.toString();
                    lines.add(builtLine);
                    lastColors = ChatColor.getLastColors(builtLine);
                    currentLine.setLength(0);
                    currentLine.append(lastColors);
                    currentVisibleLength = 0;
                }

                // Если одно слово длиннее всей строки — режем его на части
                while (wordLength > maxLineLength) {
                    String part = word.substring(0, Math.min(word.length(), maxLineLength));
                    lines.add(lastColors + part);
                    lastColors = ChatColor.getLastColors(lastColors + part);
                    word = word.substring(part.length());
                    String remStripped = ChatColorUtils.stripColors(word);
                    wordLength = remStripped != null ? remStripped.length() : word.length();
                }
            }

            if (currentVisibleLength > 0) {
                currentLine.append(" ");
                currentVisibleLength++;
            }
            currentLine.append(word);
            currentVisibleLength += wordLength;
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return String.join("\n", lines);
    }

    public void removeBubble(UUID playerId) {
        BubbleInstance instance = activeBubbles.remove(playerId);
        if (instance != null) {
            instance.destroy();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeBubble(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        removeBubble(event.getEntity().getUniqueId());
    }

    /**
     * Очищает все активные баблы (используется при reload и disable).
     */
    public void cleanup() {
        for (BubbleInstance instance : activeBubbles.values()) {
            instance.destroy();
        }
        activeBubbles.clear();
    }

    /**
     * Полная выгрузка менеджера вместе со слушателями событий.
     */
    public void shutdown() {
        cleanup();
        HandlerList.unregisterAll(this);
    }

    private static class BubbleInstance {
        private final TextDisplay display;
        private final BukkitTask task;

        public BubbleInstance(TextDisplay display, BukkitTask task) {
            this.display = display;
            this.task = task;
        }

        public void destroy() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
    }
}