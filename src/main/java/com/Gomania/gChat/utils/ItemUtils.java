package com.Gomania.gChat.utils;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ItemUtils {

    public static TextComponent convertItemToComponent(ItemStack item, String format) {
        return convertItemToComponent(item, format, "§c(Нет предмета)", "§7(Зачаровано)");
    }

    public static TextComponent convertItemToComponent(ItemStack item, String format, String itemNoneText, String itemEnchantedText) {
        if (item == null || item.getType() == Material.AIR) {
            return new TextComponent(TextComponent.fromLegacyText(itemNoneText));
        }

        ItemMeta meta = item.getItemMeta();
        String displayName;

        if (meta != null && meta.hasDisplayName() && !meta.getDisplayName().trim().isEmpty()) {
            displayName = meta.getDisplayName();
        } else {
            displayName = formatItemName(item.getType());
        }

        int amount = item.getAmount();

        String text = format
                .replace("%amount%", String.valueOf(amount))
                .replace("%item_name%", displayName);

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(ChatColorUtils.translateColors(text, true, true, true)));

        StringBuilder hoverText = new StringBuilder();
        hoverText.append(displayName).append("\n");

        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                hoverText.append(line).append("\n");
            }
        }

        if (meta != null && !meta.hasLore() && meta.hasEnchants()) {
            hoverText.append(itemEnchantedText);
        }

        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(hoverText.toString().trim()).create()));

        return component;
    }

    public static TextComponent convertInventoryToComponent(Player player, ItemStack[] contents, String format, String header, String emptyText) {
        String text = format.replace("%player_name%", player.getName());
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(ChatColorUtils.translateColors(text, true, true, true)));

        StringBuilder hoverText = new StringBuilder();
        header = header.replace("%player_name%", player.getName());
        hoverText.append(ChatColorUtils.translateColors(header, true, true, true)).append("\n");

        Map<Material, Integer> items = new HashMap<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                items.put(item.getType(), items.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (items.isEmpty()) {
            hoverText.append(ChatColorUtils.translateColors(emptyText, true, true, true));
        } else {
            for (Map.Entry<Material, Integer> entry : items.entrySet()) {
                hoverText.append("§7- §f").append(formatItemName(entry.getKey())).append(" §8x").append(entry.getValue()).append("\n");
            }
        }

        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(hoverText.toString().trim()).create()));
        return component;
    }

    private static String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}