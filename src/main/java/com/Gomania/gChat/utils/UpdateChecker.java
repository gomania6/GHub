package com.Gomania.gChat.utils;

import com.Gomania.gChat.GChat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class UpdateChecker {

    private final GChat plugin;
    private final String projectId;
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = "";

    public UpdateChecker(GChat plugin, String projectId) {
        this.plugin = plugin;
        this.projectId = (projectId == null || projectId.trim().isEmpty() || projectId.equalsIgnoreCase("gchat"))
                ? GChat.MODRINTH_PROJECT_ID
                : projectId.trim();
    }

    public void checkForUpdates(Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URI uri = URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version");
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Gomania/gChat/" + plugin.getDescription().getVersion());

                if (connection.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

                        if (!jsonArray.isEmpty()) {
                            JsonObject latest = jsonArray.get(0).getAsJsonObject();
                            String versionNumber = latest.get("version_number").getAsString();
                            String currentVersion = plugin.getDescription().getVersion();

                            if (isNewerVersion(currentVersion, versionNumber)) {
                                this.updateAvailable = true;
                                this.latestVersion = versionNumber;
                                callback.accept(true);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось проверить обновления на Modrinth: " + e.getMessage());
            }
            callback.accept(false);
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currParts = current.replaceAll("[^0-9.]", "").split("\\.");
            String[] latestParts = latest.replaceAll("[^0-9.]", "").split("\\.");

            int length = Math.max(currParts.length, latestParts.length);
            for (int i = 0; i < length; i++) {
                int curr = (i < currParts.length && !currParts[i].isEmpty()) ? Integer.parseInt(currParts[i]) : 0;
                int lat = (i < latestParts.length && !latestParts[i].isEmpty()) ? Integer.parseInt(latestParts[i]) : 0;

                if (lat > curr) return true;
                if (lat < curr) return false;
            }
        } catch (Exception ignored) {
            return !current.equalsIgnoreCase(latest);
        }
        return false;
    }
}