package net.okakuh.pepelandshop.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.okakuh.pepelandshop.render.BlockHighlighter;
import net.okakuh.pepelandshop.search.SignParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigationManager {
    private static Map<Double, List<BlockPos>> currentSortedSigns = null;
    private static List<Double> currentPriceKeys = null;
    private static int currentGroupIndex = -1;
    private static int currentStackSize = 0;

    private static boolean navigationActive = false;
    private static boolean wasNextPressed = false;
    private static boolean wasPrevPressed = false;
    private static boolean wasStopPressed = false;
    private static boolean wasQuickShopPressed = false;

    public static void handleKeyNavigation(MinecraftClient client) {
        if (!navigationActive) return;

        // Следующая группа - используем KeyBindManager
        if (KeyBindManager.isPressed("group_next") && !wasNextPressed) {
            wasNextPressed = true;
            nextGroup();
        } else if (!KeyBindManager.isPressed("group_next")) {
            wasNextPressed = false;
        }

        // Предыдущая группа - используем KeyBindManager
        if (KeyBindManager.isPressed("group_previous") && !wasPrevPressed) {
            wasPrevPressed = true;
            previousGroup();
        } else if (!KeyBindManager.isPressed("group_previous")) {
            wasPrevPressed = false;
        }

        // Остановка навигации - используем KeyBindManager
        if (KeyBindManager.isPressed("end_navigation") && !wasStopPressed) {
            wasStopPressed = true;
            stopNavigation();
        } else if (!KeyBindManager.isPressed("end_navigation")) {
            wasStopPressed = false;
        }
    }

    public static void handleQuickShop(MinecraftClient client) {
        // ПРОВЕРЯЕМ ВКЛЮЧЕН ЛИ БЫСТРЫЙ МАГАЗИН
        if (!ConfigManager.isQuickShopEnabled()) {
            return;
        }

        // Используем KeyBindManager для быстрого магазина
        if (KeyBindManager.wasPressed("quick_shop") && !wasQuickShopPressed) {
            wasQuickShopPressed = true;
            openChatWithText(ConfigManager.getQuickShopMessage());
        } else if (!KeyBindManager.wasPressed("quick_shop")) {
            wasQuickShopPressed = false;
        }
    }

    private static void openChatWithText(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.setScreen(new net.minecraft.client.gui.screen.ChatScreen(text));
        }
    }

    public static void startNavigation(Map<Double, List<BlockPos>> sortedSigns, int stack) {
        currentSortedSigns = sortedSigns;
        currentPriceKeys = new ArrayList<>(sortedSigns.keySet());
        currentGroupIndex = 0;
        navigationActive = true;
        currentStackSize = stack;

        highlightCurrentGroup();
    }

    private static void highlightCurrentGroup() {
        if (!navigationActive || currentGroupIndex < 0 || currentGroupIndex >= currentPriceKeys.size()) {
            BlockHighlighter.clearHighlights();
            return;
        }

        Double currentPrice = currentPriceKeys.get(currentGroupIndex);
        List<BlockPos> currentGroup = currentSortedSigns.get(currentPrice);

        // Получаем цвета напрямую из ConfigManager как DyeColor
        var firstColor = ConfigManager.getFirstHighlightDyeColor();
        var secondColor = ConfigManager.getSecondHighlightDyeColor();

        BlockHighlighter.highlightBlocks(currentGroup, firstColor, secondColor);

        if (MinecraftClient.getInstance().player != null) {
            String categoryMessage = "§aКатегория: §e" + parseMessage(currentPrice, currentStackSize);
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal(categoryMessage),
                    true
            );
        }
    }

    private static void nextGroup() {
        if (!navigationActive || currentPriceKeys == null) return;

        if (currentGroupIndex < currentPriceKeys.size() - 1) {
            currentGroupIndex++;
        }
        highlightCurrentGroup();
    }

    private static void previousGroup() {
        if (!navigationActive || currentPriceKeys == null) return;

        if (currentGroupIndex > 0) {
            currentGroupIndex--;
        }
        highlightCurrentGroup();
    }

    private static void stopNavigation() {
        navigationActive = false;
        currentSortedSigns = null;
        currentPriceKeys = null;
        currentGroupIndex = -1;
        BlockHighlighter.clearHighlights();

        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cНавигация завершена"),
                    true
            );
        }
    }

    private static String parseMessage(double price, int stackAmount) {
        return SignParser.formatPriceMessage(price, stackAmount);
    }
}