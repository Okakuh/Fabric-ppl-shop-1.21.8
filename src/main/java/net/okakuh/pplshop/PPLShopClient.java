package net.okakuh.pplshop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.DyeColor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PPLShopClient implements ClientModInitializer {

    // ==================== НАСТРАИВАЕМЫЕ ПЕРЕМЕННЫЕ ====================

    // Параметры по умолчанию для команды /shop
    public static final int DEFAULT_SEARCH_RADIUS = 40;

    // Настройки поиска табличек
    public static final int MIN_Y_SEARCH = 0;      // Минимальная Y координата для поиска
    public static final int MAX_Y_SEARCH = 3;      // Максимальная Y координата для поиск

    // Ограничения параметров
    public static final int MIN_STACK_SIZE = 1;
    public static final int MAX_STACK_SIZE = 64;
    public static final int MIN_RADIUS = 1;

    // Паттерны для парсинга цен и количества
    public static final String PRICE_PATTERN = "\\d+\\s*а[а-яё]{1}";
    public static final String AMOUNT_PATTERN = "\\d+\\s*[а-яё]{2}";

    private static int currentStackSize = 0;

    // ==================== СИСТЕМА НАВИГАЦИИ ====================
    private static Map<Double, List<BlockPos>> currentSortedSigns = null;
    private static List<Double> currentPriceKeys = null;
    private static int currentGroupIndex = -1;
    private static boolean navigationActive = false;

    // Флаги для защиты от множественных нажатий
    private static boolean wasAltWPressed = false;
    private static boolean wasAltSPressed = false;
    private static boolean wasUpPressed = false;
    private static boolean wasDownPressed = false;
    private static boolean wasMiddleMousePressed = false;


    // ==================== КОНЕЦ НАСТРАИВАЕМЫХ ПЕРЕМЕННЫХ ====================

    @Override
    public void onInitializeClient() {
        // Регистрируем рендер подсветки
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            BlockHighlighter.render(context);
        });

        // Обработчик тиков для навигаци
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;
            handleKeyNavigation(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Команда /shop stack search_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("search_pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                                int radius = DEFAULT_SEARCH_RADIUS;
                                                return executeShop(context, searchPattern, stack, radius, false);
                                            })
                                    )
                            )
            );

            // Команда /shop_radius radius stack search_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_radius")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(MIN_RADIUS))
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("search_pattern", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        int radius = IntegerArgumentType.getInteger(context, "radius");
                                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                                        String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                                        return executeShop(context, searchPattern, stack, radius, false);
                                                    })
                                            )
                                    )
                            )
            );

            // Команда /shop_rgx stack regex_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_rgx")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("regex_pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                                int radius = DEFAULT_SEARCH_RADIUS;
                                                return executeShop(context, regexPattern, stack, radius, true);
                                            })
                                    )
                            )
            );

            // Команда /shop_rgx_radius radius stack regex_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_rgx_radius")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(MIN_RADIUS))
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("regex_pattern", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        int radius = IntegerArgumentType.getInteger(context, "radius");
                                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                                        String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                                        return executeShop(context, regexPattern, stack, radius, true);
                                                    })
                                            )
                                    )
                            )
            );
        });
    }

    private static void handleKeyNavigation(net.minecraft.client.MinecraftClient client) {
        if (!navigationActive) return;

        long window = client.getWindow().getHandle();

        // Alt + W - следующая группа
        boolean altPressed = InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_LEFT_ALT) ||
                InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_RIGHT_ALT);

        if (altPressed && InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_W) && !wasAltWPressed) {
            wasAltWPressed = true;
            nextGroup();
        } else if (!InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_W)) {
            wasAltWPressed = false;
        }

        // Alt + S - предыдущая группа
        if (altPressed && InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_S) && !wasAltSPressed) {
            wasAltSPressed = true;
            previousGroup();
        } else if (!InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_S)) {
            wasAltSPressed = false;
        }

        // Стрелка ВВЕРХ - следующая группа (без Alt)
        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_UP) && !wasUpPressed) {
            wasUpPressed = true;
            nextGroup();
        } else if (!InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_UP)) {
            wasUpPressed = false;
        }

        // Стрелка ВНИЗ - предыдущая группа (без Alt)
        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_DOWN) && !wasDownPressed) {
            wasDownPressed = true;
            previousGroup();
        } else if (!InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_DOWN)) {
            wasDownPressed = false;
        }

        // Средняя кнопка мыши для остановки навигации
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS && !wasMiddleMousePressed) {
            wasMiddleMousePressed = true;
            stopNavigation();
        } else if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_RELEASE) {
            wasMiddleMousePressed = false;
        }
    }

    // Метод для начала навигации
    private static void startNavigation(Map<Double, List<BlockPos>> sortedSigns, String pattern, int stack, int radius, int totalSigns) {
        currentSortedSigns = sortedSigns;
        currentPriceKeys = new ArrayList<>(sortedSigns.keySet());
        currentGroupIndex = 0;
        navigationActive = true;
        currentStackSize = stack;

        highlightCurrentGroup();
    }

    // Метод для подсветки текущей группы
    private static void highlightCurrentGroup() {
        if (!navigationActive || currentGroupIndex < 0 || currentGroupIndex >= currentPriceKeys.size()) {
            BlockHighlighter.clearHighlights();
            return;
        }

        Double currentPrice = currentPriceKeys.get(currentGroupIndex);
        List<BlockPos> currentGroup = currentSortedSigns.get(currentPrice);

        // Подсвечиваем таблички текущей группы
        BlockHighlighter.highlightBlocks(currentGroup, DyeColor.LIME);

        // Показываем категорию над хотбаром с использованием parseMessage
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            String categoryMessage = "§aКатегория: §e" + parseMessage(currentPrice, currentStackSize); // ИСПОЛЬЗУЕМ parseMessage
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                    Text.literal(categoryMessage),
                    true // overlay message
            );
        }
    }

    // Метод для перехода к следующей группе
    private static void nextGroup() {
        if (!navigationActive || currentPriceKeys == null) return;

        if (currentGroupIndex < currentPriceKeys.size() - 1) {
            currentGroupIndex++;
            highlightCurrentGroup();
        } else {
            // Достигли конца
            if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
                net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("§6➡ Достигнута последняя группа"),
                        true
                );
            }
        }
    }

    private static void previousGroup() {
        if (!navigationActive || currentPriceKeys == null) return;

        if (currentGroupIndex > 0) {
            currentGroupIndex--;
            highlightCurrentGroup();
        } else {
            // Достигли начала
            if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
                net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("§6⬅ Достигнута первая группа"),
                        true
                );
            }
        }
    }

    private static void stopNavigation() {
        navigationActive = false;
        currentSortedSigns = null;
        currentPriceKeys = null;
        currentGroupIndex = -1;
        BlockHighlighter.clearHighlights();

        // Сообщение о завершении над хотбаром
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cНавигация завершена"),
                    true
            );
        }
    }

    private static int executeShop(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, String pattern, int stack, int radius, boolean useRegex) {
        var source = context.getSource();

        // Выводим параметры поиска в чат
        if (useRegex) {
            source.sendFeedback(Text.literal("§aRegex Pattern: §f" + pattern));
        } else {
            source.sendFeedback(Text.literal("§aSearch Pattern: §f" + pattern));
        }
        source.sendFeedback(Text.literal("§aStack: §f" + stack));
        source.sendFeedback(Text.literal("§aRadius: §f" + radius));

        // Ищем таблички
        List<BlockPos> foundSigns = findSignsAroundPlayer(source, radius, pattern, useRegex);

        // Выводим количество найденных табличек в чат
        source.sendFeedback(Text.literal("§6Найдено табличек: §e" + foundSigns.size()));

        // Сортируем и группируем по цене
        World world = source.getWorld();
        Map<Double, List<BlockPos>> sortedSigns = sortAndGroupSignsByPrice(foundSigns, stack, world);

        // Выводим инструкции по навигации в чат
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§6=== НАВИГАЦИЯ ==="));
        source.sendFeedback(Text.literal("§eСтрелки: ВВЕРХ/ВНИЗ §7- навигация по цен. категориям")); // МЕНЯЕМ ТЕКСТ
        source.sendFeedback(Text.literal("§cBackspace §7- завершить навигацию"));
        source.sendFeedback(Text.literal("§6=================="));

        // Запускаем навигацию
        if (!sortedSigns.isEmpty()) {
            startNavigation(sortedSigns, pattern, stack, radius, foundSigns.size());
        } else {
            source.sendFeedback(Text.literal("§cНе найдено подходящих табличек для навигации"));
        }

        return 1;
    }

    private static List<BlockPos> findSignsAroundPlayer(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, int radius, String pattern, boolean useRegex) {
        List<BlockPos> foundSigns = new ArrayList<>();

        // Получаем мир и позицию игрока
        World world = source.getWorld();
        BlockPos playerPos = source.getPlayer().getBlockPos();

        // Определяем границы поиска
        int minX = playerPos.getX() - radius;
        int maxX = playerPos.getX() + radius;
        int minZ = playerPos.getZ() - radius;
        int maxZ = playerPos.getZ() + radius;

        // Используем настраиваемые Y координаты
        int minY = MIN_Y_SEARCH;
        int maxY = MAX_Y_SEARCH;

        // Ищем таблички в указанной области
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);

                    // Проверяем, является ли блок табличкой и соответствует ли поиску
                    if (blockEntity instanceof SignBlockEntity sign) {
                        if (signMatchesSearch(sign, pattern, useRegex)) {
                            foundSigns.add(pos);
                        }
                    }
                }
            }
        }

        return foundSigns;
    }

    private static boolean signMatchesSearch(SignBlockEntity sign, String pattern, boolean useRegex) {
        // Получаем текст с передней стороны таблички
        String signText = getFrontText(sign);

        if (useRegex) {
            // Используем регулярное выражение (регистрозависимое)
            return matchesRegex(signText, pattern);
        } else {
            // Используем обычный поиск (регистронезависимый)
            String signTextLower = signText.toLowerCase();
            String patternLower = pattern.toLowerCase();
            return evaluateSearchPattern(signTextLower, patternLower);
        }
    }

    private static boolean matchesRegex(String text, String regexPattern) {
        try {
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(text);
            return matcher.find();
        } catch (Exception e) {
            // Если регулярное выражение некорректное, возвращаем false
            return false;
        }
    }

    private static String getFrontText(SignBlockEntity sign) {
        String[] text = new String[4];
        for (int i = 0; i < 4; i++) {
            // Используем настраиваемую сторону таблички
            Text line = sign.getText(true).getMessage(i, false);
            text[i] = line.getString().trim();
        }
        // Объединяем все строки в один текст для поиска
        return String.join(" ", text);
    }

    private static boolean evaluateSearchPattern(String text, String pattern) {
        // Если нет логических операторов, просто ищем вхождение
        if (!pattern.contains("+") && !pattern.contains("-")) {
            return text.contains(pattern);
        }

        // Разбиваем на OR-группы (разделены -)
        String[] orGroups = pattern.split("-");

        for (String orGroup : orGroups) {
            if (evaluateAndGroup(text, orGroup.trim())) {
                return true;
            }
        }

        return false;
    }

    private static boolean evaluateAndGroup(String text, String andGroup) {
        // Если нет AND операторов, просто ищем вхождение
        if (!andGroup.contains("+")) {
            return text.contains(andGroup.trim());
        }

        // Разбиваем на AND-условия (разделены +)
        String[] andConditions = andGroup.split("\\+");

        for (String condition : andConditions) {
            String trimmedCondition = condition.trim();
            if (trimmedCondition.isEmpty()) continue;

            if (!text.contains(trimmedCondition)) {
                return false;
            }
        }

        return true;
    }

    // Новый класс для хранения информации о табличке с ценой
    private static class SignPriceInfo {
        public BlockPos position;
        public double pricePerUnit;
        public int originalPrice;
        public int amount;

        public SignPriceInfo(BlockPos position, double pricePerUnit, int originalPrice, int amount) {
            this.position = position;
            this.pricePerUnit = pricePerUnit;
            this.originalPrice = originalPrice;
            this.amount = amount;
        }
    }

    // Функция для сортировки и группировки табличек по цене
    private static Map<Double, List<BlockPos>> sortAndGroupSignsByPrice(List<BlockPos> foundSigns, int stackAmount, World world) {
        List<SignPriceInfo> signPriceInfos = new ArrayList<>();

        // Парсим цены для всех найденных табличек
        for (BlockPos pos : foundSigns) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SignBlockEntity sign) {
                double pricePerUnit = parsePricePerUnit(sign, stackAmount);
                signPriceInfos.add(new SignPriceInfo(pos, pricePerUnit, (int)pricePerUnit, 1));
            }
        }

        // Сортируем по цене за единицу (от самой дешевой к самой дорогой)
        signPriceInfos.sort((a, b) -> Double.compare(a.pricePerUnit, b.pricePerUnit));

        // Группируем по одинаковым ценам
        Map<Double, List<BlockPos>> groupedSigns = new LinkedHashMap<>();
        for (SignPriceInfo info : signPriceInfos) {
            Double price = info.pricePerUnit;
            if (!groupedSigns.containsKey(price)) {
                groupedSigns.put(price, new ArrayList<>());
            }
            groupedSigns.get(price).add(info.position);
        }

        return groupedSigns;
    }

    // Функция для парсинга цены за единицу
    private static double parsePricePerUnit(SignBlockEntity sign, int stackAmount) {
        String[] lines = getFrontTextArray(sign);
        String allLinesLower = String.join(" ", lines).toLowerCase();

        double resultPrice = 9999.0; // По умолчанию самая большая цена
        int resultAmount = 1; // По умолчанию количество 1

        if (allLinesLower.contains("бесплат")) {
            resultPrice = -1;
        } else {
            List<String> foundPricesPatterns = regexFindAll(allLinesLower, PRICE_PATTERN);
            int countPrices = foundPricesPatterns.size();

            if (countPrices == 1) {
                String foundPriceStr = foundPricesPatterns.get(0);
                int price = parsePrice(foundPriceStr);
                resultPrice = price;

                String textWithoutPrice = allLinesLower.replace(foundPriceStr, "");
                List<String> foundAmountPatterns = regexFindAll(textWithoutPrice, AMOUNT_PATTERN);

                if (!foundAmountPatterns.isEmpty()) {
                    int amount = parseAmount(foundAmountPatterns.get(0), stackAmount);
                    resultAmount = amount;
                }
                // Если количество не найдено, остаётся resultAmount = 1

            } else if (countPrices == 2) {
                double lastPricePerUnit = 0;
                for (String line : lines) {
                    List<String> linePricePatterns = regexFindAll(line.toLowerCase(), PRICE_PATTERN);

                    if (!linePricePatterns.isEmpty()) {
                        String foundPriceStr = linePricePatterns.get(0);
                        int price = parsePrice(foundPriceStr);
                        resultPrice = price;

                        String textWithoutPrice = allLinesLower.replace(foundPriceStr, "");
                        List<String> foundAmountPatterns = regexFindAll(textWithoutPrice, AMOUNT_PATTERN);

                        if (!foundAmountPatterns.isEmpty()) {
                            int amount = parseAmount(foundAmountPatterns.get(0), stackAmount);
                            resultAmount = amount;

                            double calculatedPricePerUnit = (double) price / amount;

                            if (lastPricePerUnit == 0) {
                                lastPricePerUnit = calculatedPricePerUnit;
                                resultPrice = price;
                                resultAmount = amount;
                            } else if (calculatedPricePerUnit < lastPricePerUnit) {
                                resultPrice = price;
                                resultAmount = amount;
                            }
                        }
                        // Если количество не найдено, остаётся resultAmount = 1
                    }
                }
            }
            // Если цены не найдены (countPrices == 0), остаётся resultPrice = 9999.0
        }

        if (resultAmount == 0) {
            resultAmount = 1; // Защита от деления на ноль
        }
        return (double) resultPrice / resultAmount;
    }

    // Вспомогательный метод для получения текста таблички как массива
    private static String[] getFrontTextArray(SignBlockEntity sign) {
        String[] text = new String[4];
        for (int i = 0; i < 4; i++) {
            Text line = sign.getText(true).getMessage(i, false);
            text[i] = line.getString().trim();
        }
        return text;
    }

    // Поиск всех совпадений по регулярному выражению
    private static List<String> regexFindAll(String text, String patternStr) {
        List<String> matches = new ArrayList<>();
        try {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                matches.add(matcher.group());
            }
        } catch (Exception e) {
            // Игнорируем ошибки regex
        }
        return matches;
    }

    // Парсинг цены
    private static int parsePrice(String text) {
        String digits = text.replaceAll("\\D", "");
        int price = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        String modifierWord = text.replace(digits, "").replace(" ", "").toLowerCase();

        if (modifierWord.startsWith("аб")) {
            price = price * 9;
        }
        return price;
    }

    // Парсинг количества
    private static int parseAmount(String amountText, int stackAmount) {
        String digits = amountText.replaceAll("\\D", "");
        int amount = digits.isEmpty() ? 1 : Integer.parseInt(digits);
        String modifierWord = amountText.replace(digits, "").replace(" ", "").toLowerCase();

        if (modifierWord.startsWith("ст")) {
            amount *= stackAmount;
        } else if (modifierWord.startsWith("ша")) {
            amount *= 27 * stackAmount;
        } else if (modifierWord.startsWith("м")) {
            amount *= stackAmount;
        } else if (modifierWord.startsWith("сло")) {
            if (amount == 1) {
                amount = (int) Math.ceil(stackAmount / 4.0);
            } else {
                amount *= stackAmount;
            }
        }
        return amount;
    }

    // Функция для форматирования сообщения о цене
    private static String parseMessage(double price, int stackAmount) {
        StringBuilder message = new StringBuilder();

        if (price == -1) {
            message.append("§aБЕСПЛАТНО§r"); // Зеленый для бесплатного
        } else if (price == 9999) {
            message.append("Цена не найдена!");
        } else {
            double amount = 1;
            if (price < 1 && price != -1) {
                amount = 1 / price;
                price = 1;
            }

            int priceInt = (int) price;

            // Цена алмазов - желтый
            message.append("§e").append(priceInt).append("алм.§r");

            int ab = (int) Math.floor(priceInt / 9.0);
            if (ab >= 2) {
                // АБ в скобках - золотой
                message.append("§7(§6").append(ab).append("АБ");
                priceInt -= ab * 9;
                if (priceInt > 0) {
                    message.append("§7+§e").append(priceInt);
                }
                message.append("§7)§r");
            }
            message.append("§7-§r");

            int amountInt = (int) amount;
            int stacks = (int) Math.floor(amountInt / (double) stackAmount);
            int shulkers = (int) Math.floor(amountInt / (double) (stackAmount * 27));

            if (shulkers > 0) {
                amountInt -= shulkers * 27 * stackAmount;
                stacks = (int) Math.floor(amountInt / (double) stackAmount);
            }

            if (stackAmount == 1) stacks = 0;
            if (stacks > 0) amountInt -= stacks * stackAmount;

            boolean f = false;

            // Шалкеры - фиолетовый
            if (shulkers >= 1) {
                message.append("§d").append(shulkers).append("шалк.§r");
                f = true;
            }

            // Стаки - голубой
            if (stacks >= 1) {
                if (f) message.append("§7+§r");
                message.append("§b").append(stacks).append("ст.§r");
                f = true;
            }

            // Штуки - зеленый
            if (amountInt >= 1) {
                if (f) message.append("§7+§r");
                message.append("§a").append(amountInt).append("шт.§r");
            }
        }

        return message.toString();
    }
}