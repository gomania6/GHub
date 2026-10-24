package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogManager {

    private final GChat plugin;
    private final File logFolder;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private volatile DateTimeFormatter timeFormatter;

    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private int flushTaskId = -1;

    public LogManager(GChat plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        reloadFormatter();
        cleanupOldLogs();
        startLogWriterTask();
    }

    public void reloadFormatter() {
        String timePattern = plugin.getConfig().getString("logging.timestamp-format", "[HH:mm:ss] ");
        try {
            this.timeFormatter = DateTimeFormatter.ofPattern(timePattern);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Некорректный формат времени в logging.timestamp-format, используется стандартный [HH:mm:ss] ");
            this.timeFormatter = DateTimeFormatter.ofPattern("[HH:mm:ss] ");
        }
    }

    public void log(String message) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) return;

        // DateTimeFormatter полностью потокобезопасен при вызовах из разных потоков
        String timeStr = LocalTime.now().format(timeFormatter);
        logQueue.add(timeStr + message);
    }

    private void startLogWriterTask() {
        flushTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushLogs, 100L, 100L).getTaskId();
    }

    public void flushLogs() {
        if (logQueue.isEmpty()) return;

        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        File file = new File(logFolder, "chat-" + dateStr + ".log");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            String line;
            while ((line = logQueue.poll()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось записать лог: " + e.getMessage());
        }
    }

    public void onDisable() {
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
        // Финальный синхронный сброс очереди в файл при выключении сервера
        flushLogs();
    }

    private void cleanupOldLogs() {
        if (!plugin.getConfig().getBoolean("logging.auto-delete.enabled", true)) return;

        int keepLogsDays = plugin.getConfig().getInt("logging.auto-delete.days", 30);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File[] files = logFolder.listFiles((dir, name) -> name.startsWith("chat-") && name.endsWith(".log"));
            if (files == null) return;

            long cutoffTime = System.currentTimeMillis() - ((long) keepLogsDays * 24 * 60 * 60 * 1000);

            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        plugin.getLogger().info("Удален старый лог-файл: " + file.getName());
                    }
                }
            }
        });
    }
}