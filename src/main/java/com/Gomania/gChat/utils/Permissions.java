package com.Gomania.gChat.utils;

public final class Permissions {

    // Административные и служебные
    public static final String ADMIN = "gchat.admin";
    public static final String RELOAD = "gchat.reload";
    public static final String DEBUG = "gchat.debug";
    public static final String ALERTS = "gchat.alerts";
    public static final String BROADCAST = "gchat.broadcast";
    public static final String BROADCAST_SAY = "gchat.broadcast.say";
    public static final String BROADCAST_ALL = "gchat.broadcast.*";
    public static final String SOCIAL_SPY = "gchat.socialspy";
    public static final String MUTE_CHAT = "gchat.mutechat";
    public static final String CLEAR_CHAT = "gchat.clearchat";

    // Чат и команды общения
    public static final String USE = "gchat.use";
    public static final String GLOBAL_CHAT = "gchat.global";
    public static final String LOCAL_CHAT = "gchat.local";
    public static final String CHAT_REPLY = "gchat.chat.reply";
    public static final String CHAT_MSG = "gchat.chat.msg";

    // Упоминания и цвета
    public static final String MENTION_PING = "gchat.chat.ping";
    public static final String MENTION_EVERYONE = "gchat.chat.everyone";
    public static final String SETTINGS_COLOR = "gchat.settings.color";
    public static final String COLOR_CODES = "gchat.color.codes";
    public static final String COLOR_HEX = "gchat.color.hex";
    public static final String COLOR_GRADIENT = "gchat.gradient";

    // Обходы (Bypass)
    public static final String BYPASS_ALL = "gchat.bypass";
    public static final String BYPASS_FILTER = "gchat.bypass.filter";
    public static final String BYPASS_COOLDOWN = "gchat.bypass.cooldown";
    public static final String BYPASS_CAPS = "gchat.bypass.caps";
    public static final String BYPASS_LENGTH = "gchat.bypass.length";
    public static final String BYPASS_LINKS = "gchat.link.bypass";
    public static final String BYPASS_BLOCKED_CMDS = "gchat.bcomm.bypass";
    public static final String BYPASS_IGNORE = "gchat.ignore.bypass";
    public static final String BYPASS_MUTE_CHAT = "gchat.mutechat.bypass";
    public static final String BYPASS_CLEAR_CHAT = "gchat.clearchat.bypass";

    private Permissions() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}