package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.mute.AdvancedBanMuteHook;
import com.Gomania.gChat.hooks.mute.EssentialsMuteHook;
import com.Gomania.gChat.hooks.mute.GMuteHook;
import com.Gomania.gChat.hooks.mute.MuteHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MuteManager {

    private final GChat plugin;
    private final List<MuteHook> activeHooks = new ArrayList<>();

    public MuteManager(GChat plugin) {
        this.plugin = plugin;
        detectHooks();
    }

    private void detectHooks() {
        activeHooks.clear();
        ConfigManager cfg = plugin.getConfigManager();

        plugin.getLogger().info("§e--- DEBUG: MUTE SYSTEM CHECK ---");

        // 1. Наш новый gMute
        Plugin gmute = Bukkit.getPluginManager().getPlugin("gMute");
        if (gmute != null && gmute.isEnabled()) {
            activeHooks.add(new GMuteHook(plugin));
            plugin.getLogger().info("§a[+] gMute Hook registered.");
        }

        // 2. Essentials
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        boolean essCfg = cfg.isEssentialsMuteEnabled();
        plugin.getLogger().info("§eEssentials: Plugin=" + (ess != null) + ", Config=" + essCfg);

        if (essCfg && ess != null && ess.isEnabled()) {
            activeHooks.add(new EssentialsMuteHook(plugin));
            plugin.getLogger().info("§a[+] Essentials Hook registered.");
        }

        // 3. AdvancedBan
        Plugin ab = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        boolean abCfg = cfg.isAdvancedBanMuteEnabled();
        plugin.getLogger().info("§eAdvancedBan: Plugin=" + (ab != null) + ", Config=" + abCfg);

        if (abCfg && ab != null && ab.isEnabled()) {
            activeHooks.add(new AdvancedBanMuteHook(plugin));
            plugin.getLogger().info("§a[+] AdvancedBan Hook registered.");
        }

        plugin.getLogger().info("§e------------------------------");
    }

    public boolean isPlayerMuted(Player player) {
        for (MuteHook hook : activeHooks) {
            if (hook.isMuted(player)) {
                return true;
            }
        }
        return false;
    }

    public void reload() {
        detectHooks();
    }
}