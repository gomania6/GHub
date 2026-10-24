package com.Gomania.gChat.hooks.mute;

import com.Gomania.gChat.GChat;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class AdvancedBanMuteHook implements MuteHook {

    private final GChat plugin;
    private Object punishmentManager;
    private boolean isFunctional = false;

    public AdvancedBanMuteHook(GChat plugin) {
        this.plugin = plugin;
        try {
            Class<?> managerClass = Class.forName("me.leoko.advancedban.manager.PunishmentManager");
            Method getMethod = managerClass.getMethod("get");
            this.punishmentManager = getMethod.invoke(null);

            if (this.punishmentManager != null) {
                isFunctional = true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("AdvancedBan hook init failed: " + e.getMessage());
        }
    }

    @Override
    public String getPluginName() {
        return "AdvancedBan";
    }

    @Override
    public boolean isMuted(Player player) {
        if (!isFunctional || punishmentManager == null) return false;

        UUID uuid = player.getUniqueId();
        Class<?> pmClass = punishmentManager.getClass();

        try {
            // 1. Пробуем UUID
            try {
                Method m = pmClass.getMethod("isMuted", UUID.class);
                if ((boolean) m.invoke(punishmentManager, uuid)) return true;
            } catch (Exception ignored) {}

            // 2. Пробуем String (с тире)
            String uuidStr = uuid.toString();
            try {
                Method m = pmClass.getMethod("isMuted", String.class);
                if ((boolean) m.invoke(punishmentManager, uuidStr)) return true;
            } catch (Exception ignored) {}

            // 3. Пробуем String (без тире)
            String uuidStripped = uuidStr.replace("-", "");
            try {
                Method m = pmClass.getMethod("isMuted", String.class);
                if ((boolean) m.invoke(punishmentManager, uuidStripped)) return true;
            } catch (Exception ignored) {}

        } catch (Exception e) {
            // plugin.getLogger().warning("Error checking mute: " + e.getMessage());
        }
        return false;
    }
}