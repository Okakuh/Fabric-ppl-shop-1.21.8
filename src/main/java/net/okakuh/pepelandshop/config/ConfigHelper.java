package net.okakuh.pepelandshop.config;

import net.minecraft.util.Formatting;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigHelper {
    private static final ConfigManager.Config CONFIG = ConfigManager.getConfig();
    private static final ConfigManager.Config DEFAULT_CONFIG = ConfigManager.getDefaultConfig();

    // === Радиус поиска ===
    public static int getSearchRadius() {
        return CONFIG.default_radius;
    }

    public static void setSearchRadius(int radius) {
        CONFIG.default_radius = Math.max(1, radius);
        ConfigManager.setConfig(CONFIG);
    }

    // === Размер стака ===
    public static int getStackSize() {
        return CONFIG.default_stack;
    }

    public static void setStackSize(int stackSize) {
        CONFIG.default_stack = Math.max(1, Math.min(64, stackSize));
        ConfigManager.setConfig(CONFIG);
    }

    // === Y координаты ===
    public static int getMinY() {
        List<Integer> yCoords = CONFIG.y_coords;
        return yCoords.size() > 0 ? yCoords.get(0) : DEFAULT_CONFIG.y_coords.get(0);
    }

    public static int getMaxY() {
        List<Integer> yCoords = CONFIG.y_coords;
        return yCoords.size() > 1 ? yCoords.get(1) : DEFAULT_CONFIG.y_coords.get(1);
    }

    public static void setYCoords(int minY, int maxY) {
        CONFIG.y_coords = Arrays.asList(
                Math.max(-64, Math.min(319, minY)),
                Math.max(-64, Math.min(319, maxY))
        );
        ConfigManager.setConfig(CONFIG);
    }

    // === Паттерны ===
    public static String getPricePattern() {
        return CONFIG.price_pattern;
    }

    public static void setPricePattern(String pattern) {
        CONFIG.price_pattern = pattern != null ? pattern : DEFAULT_CONFIG.price_pattern;
        ConfigManager.setConfig(CONFIG);
    }

    public static String getAmountPattern() {
        return CONFIG.amount_pattern;
    }

    public static void setAmountPattern(String pattern) {
        CONFIG.amount_pattern = pattern != null ? pattern : DEFAULT_CONFIG.amount_pattern;
        ConfigManager.setConfig(CONFIG);
    }

    // === Цвета выделения ===

    public static Formatting getFirstHighlightFormatting() {
        List<String> colors = CONFIG.highlight_colors;
        String firstColor = colors.size() > 0 ? colors.get(0) :  DEFAULT_CONFIG.highlight_colors.get(0);
        return convertColorNameToFormatting(firstColor);
    }

    public static Formatting getSecondHighlightFormatting() {
        List<String> colors = CONFIG.highlight_colors;
        String secondColor = colors.size() > 1 ? colors.get(1) :  DEFAULT_CONFIG.highlight_colors.get(1);
        return convertColorNameToFormatting(secondColor);
    }

    // ДОБАВЛЯЕМ НОВЫЕ МЕТОДЫ:
    public static Formatting getFirstHighlightColor() {
        return getFirstHighlightFormatting();
    }

    public static Formatting getSecondHighlightColor() {
        return getSecondHighlightFormatting();
    }

    public static DyeColor getFirstHighlightDyeColor() {
        return convertFormattingToDyeColor(getFirstHighlightFormatting());
    }

    public static DyeColor getSecondHighlightDyeColor() {
        return convertFormattingToDyeColor(getSecondHighlightFormatting());
    }

    public static void setHighlightColor1(String colorName) {
        List<String> colors = CONFIG.highlight_colors;
        if (colors.size() < 2) {
            colors = Arrays.asList("green", "blue");
        }
        colors.set(0, validateColorName(colorName));
        CONFIG.highlight_colors = colors;
        ConfigManager.setConfig(CONFIG);
    }

    public static void setHighlightColor2(String colorName) {
        List<String> colors = CONFIG.highlight_colors;
        if (colors.size() < 2) {
            colors = Arrays.asList("green", "blue");
        }
        colors.set(1, validateColorName(colorName));
        CONFIG.highlight_colors = colors;
        ConfigManager.setConfig(CONFIG);
    }

    // === Конвертеры цветов ===

    private static String validateColorName(String colorName) {
        try {
            DyeColor.valueOf(colorName.toUpperCase());
            return colorName.toLowerCase();
        } catch (IllegalArgumentException e) {
            return "green";
        }
    }

    private static Formatting convertColorNameToFormatting(String colorName) {
        try {
            // Сопоставляем имена цветов с Formatting
            switch (colorName.toLowerCase()) {
                case "white": return Formatting.WHITE;
                case "orange": return Formatting.GOLD;
                case "magenta": return Formatting.LIGHT_PURPLE;
                case "light_blue": return Formatting.AQUA;
                case "yellow": return Formatting.YELLOW;
                case "lime": return Formatting.GREEN;
                case "pink": return Formatting.LIGHT_PURPLE;
                case "gray": return Formatting.GRAY;
                case "light_gray": return Formatting.GRAY;
                case "cyan": return Formatting.DARK_AQUA;
                case "purple": return Formatting.DARK_PURPLE;
                case "blue": return Formatting.BLUE;
                case "brown": return Formatting.GOLD;
                case "green": return Formatting.DARK_GREEN;
                case "red": return Formatting.RED;
                case "black": return Formatting.BLACK;
                default: return Formatting.GREEN; // по умолчанию
            }
        } catch (Exception e) {
            return Formatting.GREEN;
        }
    }

    private static DyeColor convertFormattingToDyeColor(Formatting formatting) {
        if (formatting == null) return DyeColor.LIME;

        // Сопоставляем Formatting с DyeColor
        switch (formatting) {
            case WHITE: return DyeColor.WHITE;
            case GOLD: return DyeColor.ORANGE;
            case LIGHT_PURPLE: return DyeColor.MAGENTA;
            case AQUA: return DyeColor.LIGHT_BLUE;
            case YELLOW: return DyeColor.YELLOW;
            case GREEN: return DyeColor.LIME;
            case GRAY: return DyeColor.GRAY;
            case DARK_AQUA: return DyeColor.CYAN;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case BLUE: return DyeColor.BLUE;
            case DARK_GREEN: return DyeColor.GREEN;
            case RED: return DyeColor.RED;
            case BLACK: return DyeColor.BLACK;
            default: return DyeColor.LIME;
        }
    }

    // === Полная валидация конфига ===
    // В методе validateAndFixConfig используем дефолтные значения
    public static void validateAndFixConfig() {
        // Радиус
        if (CONFIG.default_radius < 1) CONFIG.default_radius = DEFAULT_CONFIG.default_radius;
        if (CONFIG.default_radius > 1000) CONFIG.default_radius = 1000;

        // Размер стака
        if (CONFIG.default_stack < 1) CONFIG.default_stack = DEFAULT_CONFIG.default_stack;
        if (CONFIG.default_stack > 64) CONFIG.default_stack = DEFAULT_CONFIG.default_stack;

        // Y координаты
        if (CONFIG.y_coords == null || CONFIG.y_coords.size() != 2) {
            CONFIG.y_coords = new ArrayList<>(DEFAULT_CONFIG.y_coords);
        } else {
            int minY = Math.max(-64, Math.min(319, CONFIG.y_coords.get(0)));
            int maxY = Math.max(-64, Math.min(319, CONFIG.y_coords.get(1)));
            if (minY > maxY) minY = maxY;
            CONFIG.y_coords = Arrays.asList(minY, maxY);
        }

        // Паттерны
        if (CONFIG.price_pattern == null || CONFIG.price_pattern.trim().isEmpty()) {
            CONFIG.price_pattern = DEFAULT_CONFIG.price_pattern;
        }
        if (CONFIG.amount_pattern == null || CONFIG.amount_pattern.trim().isEmpty()) {
            CONFIG.amount_pattern = DEFAULT_CONFIG.amount_pattern;
        }

        // Цвета
        if (CONFIG.highlight_colors == null || CONFIG.highlight_colors.size() != 2) {
            CONFIG.highlight_colors = new ArrayList<>(DEFAULT_CONFIG.highlight_colors);
        } else {
            CONFIG.highlight_colors.set(0, validateColorName(CONFIG.highlight_colors.get(0)));
            CONFIG.highlight_colors.set(1, validateColorName(CONFIG.highlight_colors.get(1)));
        }

        // Быстрый магазин
        if (CONFIG.quick_shop_message == null || CONFIG.quick_shop_message.trim().isEmpty()) {
            CONFIG.quick_shop_message = DEFAULT_CONFIG.quick_shop_message;
        }

        ConfigManager.saveConfig();
    }

    public static String getQuickShopMessage() {
        return CONFIG.quick_shop_message != null ? CONFIG.quick_shop_message : DEFAULT_CONFIG.quick_shop_message;
    }

    public static void setQuickShopMessage(String message) {
        CONFIG.quick_shop_message = message != null ? message : DEFAULT_CONFIG.quick_shop_message;
        ConfigManager.saveConfig();
    }

    // ДОБАВЛЯЕМ НОВЫЕ МЕТОДЫ:
    public static boolean isQuickShopEnabled() {
        return CONFIG.quick_shop_enabled;
    }

    public static void setQuickShopEnabled(boolean enabled) {
        CONFIG.quick_shop_enabled = enabled;
        ConfigManager.saveConfig();
    }
}