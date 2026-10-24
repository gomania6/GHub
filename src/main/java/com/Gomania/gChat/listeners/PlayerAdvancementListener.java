package com.Gomania.gChat.listeners;

import com.Gomania.gChat.GChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class PlayerAdvancementListener implements Listener {

    private final GChat plugin;

    public PlayerAdvancementListener(GChat plugin) {
        this.plugin = plugin;

        if (plugin.getConfig().getBoolean("advancements.disable-vanilla-announcements", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (plugin.getConfig().getBoolean("advancements.disable-vanilla-announcements", true)) {
            event.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        NamespacedKey nsKey = event.getAdvancement().getKey();

        String namespace = nsKey.getNamespace();
        String key = nsKey.getKey();

        // 1. Игнорируем открытие рецептов
        if (key.startsWith("recipes/")) {
            return;
        }

        // 2. Игнорируем ВСЕ достижения из датапаков и сторонних модов.
        // Если пространство имен не "minecraft", значит это кастомное/техническое достижение.
        if (!namespace.equals("minecraft")) {
            return;
        }

        BaseComponent advTitle;
        BaseComponent advDesc;
        BaseComponent hoverTitle;

        // Если это стандартное достижение Minecraft
        if (namespace.equals("minecraft")) {
            String translatableTitleKey = "advancements." + key.replace('/', '.') + ".title";
            String translatableDescKey = "advancements." + key.replace('/', '.') + ".description";

            advTitle = new TranslatableComponent(translatableTitleKey);
            advTitle.setColor(ChatColor.GREEN);

            hoverTitle = new TranslatableComponent(translatableTitleKey);
            hoverTitle.setColor(ChatColor.GREEN);

            advDesc = new TranslatableComponent(translatableDescKey);
            advDesc.setColor(ChatColor.WHITE);
        }
        // Если это кастомное достижение (Датапаки / Моды)
        else {
            // Превращаем "intro_song/welcome_toast" в "Welcome toast"
            String[] parts = key.split("/");
            String rawName = parts[parts.length - 1].replace("_", " ");
            String niceName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);

            advTitle = new TextComponent(niceName);
            advTitle.setColor(ChatColor.GREEN);

            hoverTitle = new TextComponent(niceName);
            hoverTitle.setColor(ChatColor.GREEN);

            advDesc = new TextComponent("Достижение (" + namespace + ")");
            advDesc.setColor(ChatColor.GRAY);
        }

        advTitle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[]{
                hoverTitle, new TextComponent("\n"), advDesc
        })));

        TranslatableComponent message = new TranslatableComponent("chat.type.advancement.task");
        message.addWith(new TextComponent(player.getName()));
        message.addWith(advTitle);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getSettingsManager().getSettings(p.getUniqueId()).showAdvancements) {
                p.spigot().sendMessage(message);
            }
        }

        Bukkit.getConsoleSender().sendMessage("§7[Advancement] " + player.getName() + " -> " + namespace + ":" + key);
    }
}