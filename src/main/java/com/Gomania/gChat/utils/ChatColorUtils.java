package com.Gomania.gChat.utils;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитарный класс для работы с цветами, градиентами и HEX-кодами.
 */
public class ChatColorUtils {

    // Паттерн для градиентов: {#RRGGBB>}текст{#RRGGBB<}
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("\\{#([0-9A-Fa-f]{6})>\\}(.*?)\\{#([0-9A-Fa-f]{6})<\\}");

    // Паттерн для HEX: &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[&§]#([A-Fa-f0-9]{6})");

    /**
     * Основной метод перевода цветов.
     */
    public static String translateColors(String text, boolean allowHex, boolean allowStandard, boolean allowGradients) {
        if (text == null || text.isEmpty()) return "";

        // 1. Сначала обрабатываем градиенты
        if (allowGradients) {
            text = applyGradients(text);
        }

        // 2. Затем HEX цвета (&#RRGGBB)
        if (allowHex) {
            text = applyHexColors(text);
        }

        // 3. Стандартные цвета (&a, &l...)
        if (allowStandard) {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }

        return text;
    }

    /**
     * Перегрузка для упрощенного вызова.
     */
    public static String translateColors(String text, boolean allowColors, boolean allowGradients) {
        return translateColors(text, allowColors, allowColors, allowGradients);
    }

    public static String stripColors(String input) {
        if (input == null) return null;

        // 1. Убираем градиенты {#HEX>}text{#HEX<} -> text
        input = input.replaceAll("\\{#([0-9A-Fa-f]{6})>\\}(.*?)\\{#([0-9A-Fa-f]{6})<\\}", "$2");

        // 2. Убираем сырые HEX &#RRGGBB
        input = input.replaceAll("&#[A-Fa-f0-9]{6}", "");

        // 3. Убираем сырые коды &a, &l, а также &x
        input = input.replaceAll("&[0-9a-fk-orA-FK-ORxX]", "");

        // 4. Убираем переведенные HEX коды BungeeCord (§x§r§r§g§g§b§b)
        input = input.replaceAll("§x(§[0-9A-Fa-f]){6}", "");

        // 5. Убираем стандартные переведенные коды (§a, §l)
        return ChatColor.stripColor(input);
    }



    private static String applyHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            try {
                String hexCode = matcher.group(1);
                String replacement = ChatColor.of("#" + hexCode).toString();
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String content = matcher.group(2);
            String endHex = matcher.group(3);

            String gradientText = createGradient(content, startHex, endHex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String createGradient(String text, String startHex, String endHex) {
        try {
            Color startColor = Color.decode("#" + startHex);
            Color endColor = Color.decode("#" + endHex);
            StringBuilder gradient = new StringBuilder();

            // Считаем только видимые символы для плавности градиента
            int visibleLength = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                // ИГНОРИРУЕМ и амперсанд, и знак параграфа
                if ((c == '&' || c == '§') && i + 1 < text.length()) {
                    i++; // Пропускаем код цвета
                } else {
                    visibleLength++;
                }
            }

            int currentStep = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                // Если встречаем код форматирования, добавляем его без изменения цвета
                if ((c == '&' || c == '§') && i + 1 < text.length()) {
                    gradient.append(c).append(text.charAt(i + 1));
                    i++;
                    continue;
                }

                float ratio = (visibleLength <= 1) ? 0 : (float) currentStep / (visibleLength - 1);
                Color color = interpolateColor(startColor, endColor, ratio);

                gradient.append(ChatColor.of(toColor(color))).append(c);
                currentStep++;
            }
            return gradient.toString();
        } catch (Exception e) {
            return text;
        }
    }

    private static Color toColor(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color interpolateColor(Color start, Color end, float ratio) {
        int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    public static String colorToMinecraftHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}