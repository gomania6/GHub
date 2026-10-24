package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.utils.ChatColorUtils;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.entity.Player;

public class TownyHook {

    private final GChat plugin;
    private final TownyAPI towny;

    public TownyHook(GChat plugin) {
        this.plugin = plugin;
        this.towny = TownyAPI.getInstance();
    }

    public String getTownyTag(Player player) {
        if (player == null) return "";
        ConfigManager config = plugin.getConfigManager();

        Resident res = towny.getResident(player.getUniqueId());
        if (res == null || !res.hasTown()) return "";

        try {
            Town town = res.getTown();
            Nation nation = town.hasNation() ? town.getNation() : null;

            String townName = resolveName(town);

            if (nation != null) {
                String nationName = resolveName(nation);
                String format = config.getTownyFormatBoth();
                // Безопасное форматирование с поддержкой HEX и градиентов
                return ChatColorUtils.translateColors(String.format(format, nationName, townName), true, true, true);
            } else {
                String format = config.getTownyFormatTown();
                return ChatColorUtils.translateColors(String.format(format, townName), true, true, true);
            }

        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return "";
    }

    public String getTownTagOnly(Player player) {
        if (player == null) return "";
        try {
            Resident res = towny.getResident(player.getUniqueId());
            if (res != null && res.hasTown()) {
                String townName = resolveName(res.getTown());
                String format = plugin.getConfigManager().getTownyFormatTown();
                return ChatColorUtils.translateColors(String.format(format, townName), true, true, true);
            }
        } catch (Exception ignored) {}
        return "";
    }

    public String getNationTagOnly(Player player) {
        if (player == null) return "";
        try {
            Resident res = towny.getResident(player.getUniqueId());
            if (res != null && res.hasTown() && res.getTown().hasNation()) {
                String nationName = resolveName(res.getTown().getNation());
                String format = plugin.getConfigManager().getTownyFormatNation();
                return ChatColorUtils.translateColors(String.format(format, nationName), true, true, true);
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Получает имя или тег объекта, проверяя конкретный тип (Town или Nation).
     */
    private String resolveName(TownyObject obj) {
        boolean preferTag = plugin.getConfigManager().isTownyPreferTag();
        String tag = null;

        // Явно приводим типы, так как TownyObject не имеет метода getTag() в новых версиях API
        if (obj instanceof Town) {
            Town t = (Town) obj;
            if (t.hasTag()) tag = t.getTag();
        } else if (obj instanceof Nation) {
            Nation n = (Nation) obj;
            if (n.hasTag()) tag = n.getTag();
        }

        if (preferTag && tag != null && !tag.isEmpty()) {
            return tag;
        }
        return obj.getName();
    }
}