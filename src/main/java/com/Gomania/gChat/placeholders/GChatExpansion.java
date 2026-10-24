package com.Gomania.gChat.placeholders;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.TownyHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GChatExpansion extends PlaceholderExpansion {

    private final GChat plugin;

    public GChatExpansion(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gchat";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // Towny Placeholders
        if (plugin.getDependencyManager().hasTowny() && plugin.getTownyHook() != null) {
            TownyHook hook = plugin.getTownyHook();
            switch (params.toLowerCase()) {
                case "towny":
                    return hook.getTownyTag(player);
                case "towny_town": // Изменил на underscore для читаемости, но можно оставить townytown
                    return hook.getTownTagOnly(player);
                case "towny_nation":
                    return hook.getNationTagOnly(player);
            }
        }

        return null;
    }
}