package com.Gomania.gChat.commands;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.PlayerSettingsManager.PlayerSettings;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsCommand implements CommandExecutor, TabCompleter {

    private final GChat plugin;

    public SettingsCommand(GChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("player-settings.enabled", true)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("settings-disabled"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("settings-players-only"));
            return true;
        }

        Player player = (Player) sender;
        PlayerSettings settings = plugin.getSettingsManager().getSettings(player.getUniqueId());

        if (args.length == 0) {
            if (plugin.getSettingsGuiProvider() != null) {
                plugin.getSettingsGuiProvider().openSettings(player);
                return true;
            }

            String statusOn = plugin.getMessageManager().getMessage("settings-status-on");
            String statusOff = plugin.getMessageManager().getMessage("settings-status-off");

            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-header"));
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-deaths").replace("{status}", settings.showDeathMessages ? statusOn : statusOff));
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-joinquit").replace("{status}", settings.showJoinQuitMessages ? statusOn : statusOff));
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-advancements").replace("{status}", settings.showAdvancements ? statusOn : statusOff));
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-global").replace("{status}", settings.globalChatEnabled ? statusOn : statusOff));
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-sound"));

            String defaultColor = plugin.getMessageManager().getMessage("settings-color-default");
            String currentColor = settings.chatColorTheme.isEmpty() ? defaultColor : ChatColorUtils.translateColors(settings.chatColorTheme + "Text", true, true, true);
            player.sendMessage(plugin.getMessageManager().getMessage("settings-menu-color").replace("{color}", currentColor));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "deaths":
                settings.showDeathMessages = !settings.showDeathMessages;
                player.sendMessage(plugin.getMessageManager().getMessage(settings.showDeathMessages ? "settings-deaths-on" : "settings-deaths-off"));
                break;

            case "joinquit":
                settings.showJoinQuitMessages = !settings.showJoinQuitMessages;
                player.sendMessage(plugin.getMessageManager().getMessage(settings.showJoinQuitMessages ? "settings-joinquit-on" : "settings-joinquit-off"));
                break;

            case "advancements":
                settings.showAdvancements = !settings.showAdvancements;
                player.sendMessage(plugin.getMessageManager().getMessage(settings.showAdvancements ? "settings-adv-on" : "settings-adv-off"));
                break;

            case "global":
                settings.globalChatEnabled = !settings.globalChatEnabled;
                player.sendMessage(plugin.getMessageManager().getMessage(settings.globalChatEnabled ? "settings-global-on" : "settings-global-off"));
                break;

            case "sound":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-sound-usage"));
                    return true;
                }
                String chosenSound = args[1].toUpperCase();
                if (chosenSound.equals("XP") || chosenSound.equals("PLING") || chosenSound.equals("ANVIL") || chosenSound.equals("NONE") || chosenSound.equals("DEFAULT")) {
                    settings.mentionSound = chosenSound;
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-sound-changed").replace("{sound}", chosenSound));
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-sound-invalid"));
                }
                break;

            case "color":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-color-usage"));
                    return true;
                }

                String mode = args[1].toLowerCase();
                if (!mode.equals("chat") && !mode.equals("full")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-color-usage"));
                    return true;
                }

                if (args.length < 3 || args[2].equalsIgnoreCase("default") || args[2].equalsIgnoreCase("none")) {
                    settings.chatColorTheme = "";
                    settings.chatColorMode = "chat";
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-color-reset"));
                } else {
                    if (!player.hasPermission("gchat.settings.color")) {
                        player.sendMessage(plugin.getMessageManager().getMessage("settings-no-permission-color"));
                        return true;
                    }
                    settings.chatColorMode = mode;
                    settings.chatColorTheme = args[2];
                    String typeStr = mode.equals("full") ? plugin.getMessageManager().getMessage("settings-color-type-full") : plugin.getMessageManager().getMessage("settings-color-type-chat");
                    String example = ChatColorUtils.translateColors(args[2] + "Text", true, true, true);
                    player.sendMessage(plugin.getMessageManager().getMessage("settings-color-changed").replace("{type}", typeStr).replace("{example}", example));
                }
                break;

            default:
                player.sendMessage(plugin.getMessageManager().getMessage("settings-unknown"));
                return true;
        }

        plugin.getSettingsManager().saveSettings(player.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!plugin.getConfig().getBoolean("player-settings.enabled", true)) return Collections.emptyList();

        if (args.length == 1) {
            completions.add("deaths");
            completions.add("joinquit");
            completions.add("advancements");
            completions.add("global");
            completions.add("sound");
            if (sender.hasPermission("gchat.settings.color")) completions.add("color");
            return filterCompletions(completions, args[0]);

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("sound")) {
                completions.add("xp");
                completions.add("pling");
                completions.add("anvil");
                completions.add("none");
                completions.add("default");
                return filterCompletions(completions, args[1]);
            } else if (args[0].equalsIgnoreCase("color") && sender.hasPermission("gchat.settings.color")) {
                completions.add("chat");
                completions.add("full");
                return filterCompletions(completions, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("color") && sender.hasPermission("gchat.settings.color")) {
                completions.add("default");
                completions.add("none");
                completions.add("&#000000");
                completions.add("&#FF0000");
                completions.add("&c");
                return filterCompletions(completions, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> list, String input) {
        List<String> result = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lowerInput)) result.add(s);
        }
        return result;
    }
}