package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.hooks.LuckPermsHook;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.managers.CooldownManager;
import com.Gomania.gChat.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Обрабатывает команду /gchat.
 */
public class GChatCommand implements CommandExecutor, TabCompleter {

    private final GChat plugin;
    private final MessageManager messages;
    private final ConfigManager config;
    private final CooldownManager reloadCooldown = new CooldownManager();

    public GChatCommand(GChat plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "help":
                sendHelp(sender);
                return true;
            case "debug":
                return handleDebug(sender);
            default:
                sender.sendMessage(messages.getMessage("unknown-command"));
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("gchat.reload")) {
            sender.sendMessage(messages.getMessage("no-permission"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            int cd = config.getReloadCooldown();
            if (!reloadCooldown.check(player.getUniqueId(), cd)) {
                long remaining = reloadCooldown.getRemaining(player.getUniqueId(), cd);
                player.sendMessage(messages.getMessage("reload-cooldown").replace("{time}", String.valueOf(remaining)));
                return true;
            }
        }

        // Вызываем централизованный метод перезагрузки
        plugin.reloadPlugin();
        sender.sendMessage(messages.getMessage("config-reloaded"));
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("players-only"));
            return true;
        }
        if (!sender.hasPermission("gchat.debug")) {
            sender.sendMessage(messages.getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        LuckPermsHook lpHook = plugin.getLuckPermsHook();

        player.sendMessage("§6=== gChat Debug ===");
        player.sendMessage("§7Username: §e" + player.getName());

        if (lpHook == null) {
            player.sendMessage("§cLuckPerms не найден или не инициализирован.");
        } else {
            try {
                Set<String> validGroups = config.getGroupFormatKeys();
                String group = lpHook.getPlayerGroup(player.getUniqueId(), validGroups);
                String suffix = lpHook.getPlayerSuffix(player.getUniqueId());

                player.sendMessage("§7Primary Group (from cache): §e" + (group != null ? group : "default"));
                player.sendMessage("§7Suffix (from cache): §f" + (suffix != null && !suffix.isEmpty() ? suffix : "none"));
            } catch (Exception e) {
                player.sendMessage("§cОшибка при получении данных LuckPerms: " + e.getMessage());
            }
        }

        player.sendMessage("§7PAPI: §e" + plugin.getDependencyManager().hasPlaceholderAPI());
        player.sendMessage("§7DiscordSRV: §e" + plugin.getDependencyManager().hasDiscordSRV());

        return true;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = messages.getHelpMessage();
        for (String line : helpLines) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("gchat.reload")) completions.add("reload");
            if (sender.hasPermission("gchat.debug")) completions.add("debug");
            completions.add("help");

            return completions.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}