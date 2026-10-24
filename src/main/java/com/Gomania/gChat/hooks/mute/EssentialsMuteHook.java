package com.Gomania.gChat.hooks.mute;

import com.Gomania.gChat.GChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class EssentialsMuteHook implements MuteHook {

    private final Object essentialsPlugin;
    private Method getUserMethod;
    private Method isMutedMethod;
    private boolean isFunctional = false;

    public EssentialsMuteHook(GChat plugin) {
        this.essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin != null) {
            try {
                Class<?> essClass = essentialsPlugin.getClass();

                // Попытка 1: ищем getUser(Player)
                try {
                    getUserMethod = essClass.getMethod("getUser", Player.class);
                } catch (NoSuchMethodException e) {
                    // Попытка 2: ищем getUser(Object) - для новых версий/форков
                    getUserMethod = essClass.getMethod("getUser", Object.class);
                }

                if (getUserMethod != null) {
                    Class<?> userClass = getUserMethod.getReturnType();
                    isMutedMethod = userClass.getMethod("isMuted");
                    isFunctional = true;
                    plugin.getLogger().info("Essentials Mute Hook initialized successfully.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error initializing Essentials hook reflection: " + e.getMessage());
            }
        }
    }

    @Override
    public String getPluginName() {
        return "Essentials";
    }

    @Override
    public boolean isMuted(Player player) {
        if (!isFunctional || player == null) return false;
        try {
            Object user = getUserMethod.invoke(essentialsPlugin, player);
            if (user != null) {
                return (boolean) isMutedMethod.invoke(user);
            }
        } catch (Exception ignored) {}
        return false;
    }
}