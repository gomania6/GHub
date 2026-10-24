package com.Gomania.gChat.managers;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelManager {

    private final GChat plugin;
    private final Map<String, Channel> channels = new HashMap<>();

    public ChannelManager(GChat plugin) {
        this.plugin = plugin;
        loadChannels();
    }

    public void loadChannels() {
        // Сначала очищаем старые команды, если это перезагрузка
        if (!channels.isEmpty()) {
            unregisterAll();
        }
        channels.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("channels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String commandName = section.getString(key + ".command");
            List<String> aliases = section.getStringList(key + ".aliases");
            String permission = section.getString(key + ".permission");
            String format = section.getString(key + ".format");
            int range = section.getInt(key + ".range", -1);
            double cost = section.getDouble(key + ".cost", 0.0);

            Channel channel = new Channel(key, commandName, aliases, permission, format, range, cost);
            channels.put(key, channel);
            registerCommand(channel);
        }
        // Обновляем автокомплит команд у всех игроков, чтобы новые каналы появились в TAB
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        });
    }

    public void unregisterAll() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            for (Channel channel : channels.values()) {
                Command cmd = commandMap.getCommand(channel.command);
                if (cmd != null) {
                    cmd.unregister(commandMap);
                    knownCommands.remove(channel.command);
                    knownCommands.remove("gchat:" + channel.command);
                }

                if (channel.aliases != null) {
                    for (String alias : channel.aliases) {
                        Command aliasCmd = commandMap.getCommand(alias);
                        if (aliasCmd != null) {
                            aliasCmd.unregister(commandMap);
                            knownCommands.remove(alias);
                            knownCommands.remove("gchat:" + alias);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при выгрузке команд каналов: " + e.getMessage());
        }
    }

    private void registerCommand(Channel channel) {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            Command cmd = new Command(channel.command) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("players-only"));
                        return true;
                    }
                    if (args.length == 0) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("channel-usage").replace("%cmd%", label));
                        return true;
                    }

                    handleChannelMessage((Player) sender, channel, String.join(" ", args));
                    return true;
                }
            };

            if (channel.aliases != null && !channel.aliases.isEmpty()) {
                cmd.setAliases(channel.aliases);
            }
            // Убрали cmd.setPermission(), так как мы проверяем права вручную в handleChannelMessage
            // Это позволит выводить красивое сообщение об ошибке, а не стандартное красное.

            commandMap.register("gchat", cmd);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка регистрации команды канала " + channel.name + ": " + e.getMessage());
        }
    }

    private void handleChannelMessage(Player player, Channel channel, String message) {
        if (plugin.getMuteManager().isPlayerMuted(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("muted-message"));
            return;
        }

        // Проверка прав отправителя
        boolean hasPermissionRestriction = channel.permission != null && !channel.permission.isEmpty();
        if (hasPermissionRestriction && !player.hasPermission(channel.permission)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }

        String format = channel.format
                .replace("%player_name%", player.getName())
                .replace("%message%", message);

        format = ChatColorUtils.translateColors(format, true, true, true);

        // Отправка
        if (channel.range == -1) {
            // Глобально - отправляем только тем, у кого ЕСТЬ ПРАВА на этот канал
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!hasPermissionRestriction || p.hasPermission(channel.permission)) {
                    p.sendMessage(format);
                }
            }
        } else {
            // Локально - проверяем и дистанцию, и права
            double radiusSq = channel.range * channel.range;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(player.getWorld()) && p.getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                    if (!hasPermissionRestriction || p.hasPermission(channel.permission)) {
                        p.sendMessage(format);
                    }
                }
            }
        }

        plugin.getLogManager().log("[" + channel.name.toUpperCase() + "] " + player.getName() + ": " + message);
    }

    private static class Channel {
        String name;
        String command;
        List<String> aliases;
        String permission;
        String format;
        int range;
        double cost;

        public Channel(String name, String command, List<String> aliases, String permission, String format, int range, double cost) {
            this.name = name;
            this.command = command;
            this.aliases = aliases;
            this.permission = permission;
            this.format = format;
            this.range = range;
            this.cost = cost;
        }
    }
}