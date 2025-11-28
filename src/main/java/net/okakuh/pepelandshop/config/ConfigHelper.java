package net.okakuh.pepelandshop.config;

import net.minecraft.util.Formatting;
import net.minecraft.util.DyeColor;

import java.util.Arrays;
import java.util.List;

public class ConfigHelper {
    private static final ConfigManager.Config CONFIG = ConfigManager.getConfig();

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
        return yCoords.size() > 0 ? yCoords.get(0) : 0;
    }

    public static int getMaxY() {
        List<Integer> yCoords = CONFIG.y_coords;
        return yCoords.size() > 1 ? yCoords.get(1) : 3;
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
        CONFIG.price_pattern = pattern != null ? pattern : "\\d+\\s*а[а-яё]{1}";
        ConfigManager.setConfig(CONFIG);
    }

    public static String getAmountPattern() {
        return CONFIG.amount_pattern;
    }

    public static void setAmountPattern(String pattern) {
        CONFIG.amount_pattern = pattern != null ? pattern : "\\d+\\s*[а-яё]{2}";
        ConfigManager.setConfig(CONFIG);
    }

    // === Цвета выделения ===

    public static Formatting getFirstHighlightFormatting() {
        List<String> colors = CONFIG.highlight_colors;
        String firstColor = colors.size() > 0 ? colors.get(0) : "green";
        return convertColorNameToFormatting(firstColor);
    }

    public static Formatting getSecondHighlightFormatting() {
        List<String> colors = CONFIG.highlight_colors;
        String secondColor = colors.size() > 1 ? colors.get(1) : "blue";
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
            // Проверяем, что цвет существует в DyeColor
            DyeColor.valueOf(colorName.toUpperCase());
            return colorName.toLowerCase();
        } catch (IllegalArgumentException e) {
            return "green"; // значение по умолчанию
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
    public static void validateAndFixConfig() {
        // Радиус
        if (CONFIG.default_radius < 1) CONFIG.default_radius = 40;
        if (CONFIG.default_radius > 1000) CONFIG.default_radius = 1000;

        // Размер стака
        if (CONFIG.default_stack < 1) CONFIG.default_stack = 1;
        if (CONFIG.default_stack > 64) CONFIG.default_stack = 64;

        // Y координаты
        if (CONFIG.y_coords == null || CONFIG.y_coords.size() != 2) {
            CONFIG.y_coords = Arrays.asList(0, 3);
        } else {
            int minY = Math.max(-64, Math.min(319, CONFIG.y_coords.get(0)));
            int maxY = Math.max(-64, Math.min(319, CONFIG.y_coords.get(1)));
            if (minY > maxY) minY = maxY;
            CONFIG.y_coords = Arrays.asList(minY, maxY);
        }

        // Паттерны
        if (CONFIG.price_pattern == null || CONFIG.price_pattern.trim().isEmpty()) {
            CONFIG.price_pattern = "\\d+\\s*а[а-яё]{1}";
        }
        if (CONFIG.amount_pattern == null || CONFIG.amount_pattern.trim().isEmpty()) {
            CONFIG.amount_pattern = "\\d+\\s*[а-яё]{2}";
        }

        // Цвета
        if (CONFIG.highlight_colors == null || CONFIG.highlight_colors.size() != 2) {
            CONFIG.highlight_colors = Arrays.asList("green", "blue");
        } else {
            CONFIG.highlight_colors.set(0, validateColorName(CONFIG.highlight_colors.get(0)));
            CONFIG.highlight_colors.set(1, validateColorName(CONFIG.highlight_colors.get(1)));
        }

        ConfigManager.setConfig(CONFIG);
    }
}