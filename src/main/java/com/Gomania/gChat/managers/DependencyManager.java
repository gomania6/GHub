package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;

/**
 * Проверяет наличие и инициализирует API сторонних плагинов.
 */
public class DependencyManager {

    private final GChat plugin;

    private boolean hasPlaceholderAPI = false;
    private boolean hasLuckPerms = false;
    private boolean hasDiscordSRV = false;
    private boolean hasEssentials = false;
    private boolean hasTowny = false; // NEW

    private Object luckPermsAPI = null;

    public DependencyManager(GChat plugin) {
        this.plugin = plugin;
    }

    public void checkDependencies() {
        // PlaceholderAPI
        hasPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (hasPlaceholderAPI) plugin.getLogger().info("PlaceholderAPI found! Placeholders enabled.");

        // LuckPerms
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
                luckPermsAPI = Bukkit.getServicesManager().getRegistration(luckPermsClass).getProvider();
                hasLuckPerms = (luckPermsAPI != null);
                if (hasLuckPerms) plugin.getLogger().info("LuckPerms found! Group support enabled.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to initialize LuckPerms API: " + e.getMessage());
                hasLuckPerms = false;
                luckPermsAPI = null;
            }
        } else {
            hasLuckPerms = false;
            luckPermsAPI = null;
        }

        // DiscordSRV
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
            try {
                Class.forName("github.scarsz.discordsrv.DiscordSRV");
                hasDiscordSRV = true;
                plugin.getLogger().info("DiscordSRV found! Integration will be enabled if set in integrations.yml.");
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                plugin.getLogger().warning("DiscordSRV plugin found, but required classes are missing. Integration disabled.");
                hasDiscordSRV = false;
            }
        } else {
            hasDiscordSRV = false;
        }

        // Essentials
        hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
        if (hasEssentials) {
            plugin.getLogger().info("Essentials found! Mute integration enabled.");
        }

        // Towny (NEW)
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            hasTowny = true;
            plugin.getLogger().info("Towny found! Town/Nation tags enabled.");
        } else {
            hasTowny = false;
        }
        // AuthMe
        hasAuthMe = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        if (hasAuthMe) {
            plugin.getLogger().info("AuthMe found! Chat commands protection enabled.");
        }
    }

    // --- Геттеры ---
    private boolean hasAuthMe = false;
    public boolean hasAuthMe() { return hasAuthMe; }
    public boolean hasPlaceholderAPI() { return hasPlaceholderAPI; }
    public boolean hasLuckPerms() { return hasLuckPerms; }
    public boolean hasDiscordSRV() { return hasDiscordSRV; }
    public boolean hasEssentials() { return hasEssentials; }
    public boolean hasTowny() { return hasTowny; } // NEW

    public Object getLuckPermsAPI() { return luckPermsAPI; }
}