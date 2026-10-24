package com.Gomania.gChat.hooks;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.utils.ChatColorUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.List;

public class DiscordSRVHook {

    private final GChat plugin;
    private final ConfigManager config;

    public DiscordSRVHook(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void subscribe() {
        try {
            DiscordSRV.api.subscribe(this);
            plugin.getLogger().info("Successfully subscribed to DiscordSRV API.");
        } catch (Exception | NoClassDefFoundError e) {
            plugin.getLogger().warning("Failed to subscribe to DiscordSRV API: " + e.getMessage());
        }
    }

    public void unsubscribe() {
        try {
            DiscordSRV.api.unsubscribe(this);
        } catch (Exception | NoClassDefFoundError ignored) {}
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessagePostProcessEvent event) {
        if (!config.isDiscordToMcEnabled()) return;
        if (event.getAuthor().isBot()) return;

        TextChannel mainChannel = DiscordSRV.getPlugin().getMainTextChannel();
        if (mainChannel == null || !event.getChannel().getId().equals(mainChannel.getId())) {
            return;
        }

        Member member = event.getMember();
        String messageContent = event.getMessage().getContentDisplay();

        String format = config.getDiscordChatFormat();
        String username = (member != null) ? member.getEffectiveName() : event.getAuthor().getName();
        String roleName = "";
        String roleColor = "";

        if (member != null) {
            List<Role> roles = member.getRoles();
            if (!roles.isEmpty()) {
                Role topRole = roles.get(0);
                roleName = topRole.getName();
                Color color = topRole.getColor();
                if (color != null) {
                    roleColor = ChatColorUtils.colorToMinecraftHex(color);
                }
            }
        }

        format = format.replace("%user%", username)
                .replace("%role_name%", roleName)
                .replace("%role_color%", roleColor)
                .replace("%message%", messageContent);

        // Цвета
        String finalMessage = ChatColorUtils.translateColors(format, true, true, true);

        // ВАЖНО: Переход в синхронный поток для работы с Bukkit API
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getChatManager().sendGlobalMessage(new TextComponent(finalMessage));
        });
    }

    public void sendToDiscord(Player player, String message) {
        if (!config.isMcToDiscordEnabled()) return;
        if (!plugin.getDependencyManager().hasDiscordSRV()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
                if (channel == null) return;

                String cleanMessage = ChatColorUtils.stripColors(message);
                String discordMessage = "**" + player.getName() + "**: " + cleanMessage;
                channel.sendMessage(discordMessage).queue();

            } catch (Exception | NoClassDefFoundError e) {
                plugin.getLogger().warning("Failed to send message to Discord: " + e.getMessage());
            }
        });
    }
}