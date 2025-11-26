package net.okakuh.pepelandshop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;

import static net.okakuh.pepelandshop.ConfigManager.*;
import net.okakuh.pepelandshop.PepelandShopSettingsScreen;

public class PepelandShopClient implements ClientModInitializer {
    // ==================== УДОБСТВО ====================
    private static int currentStackSize = 0;

    private static Map<Double, List<BlockPos>> currentSortedSigns = null;
    private static List<Double> currentPriceKeys = null;
    private static int currentGroupIndex = -1;

    private static boolean navigationActive = false;
    private static boolean shouldOpenSettings = false;

    // Флаги для защиты от множественных нажатий навигации
    private static boolean wasNextPressed = false;
    private static boolean wasPrevPressed = false;
    private static boolean wasStopPressed = false;

    @Override
    public void onInitializeClient() {
        // Загружаем конфиг при запуске
        loadConfig();

        PepelandShopKeyBinds.registerKeyBindings();

        // Регистрируем рендер подсветки
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            BlockHighlighter.render(context);
        });

        // Обработчик тиков для навигации
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;
            handleKeyNavigation(client);
            handleQuickShop(client);
            if (shouldOpenSettings) {
                shouldOpenSettings = false;
                client.setScreen(new PepelandShopSettingsScreen());
            }
        });

        // Регистрируем команды настроек
        registerSettingCommands();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("settings")
                                    .executes(context -> {
                                        shouldOpenSettings = true;
                                        return 1;
                                    })
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(1, 64)) // константа
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("search_pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                                int radius = getDefaultSearchRadius();
                                                return executeShop(context, searchPattern, stack, radius, false);
                                            })
                                    )
                            )
            );

            // Команда /shop_radius radius stack search_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_radius")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(1)) // константа
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(1, 64)) // константа
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
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(1, 64)) // константа
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("regex_pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                                int radius = getDefaultSearchRadius();
                                                return executeShop(context, regexPattern, stack, radius, true);
                                            })
                                    )
                            )
            );

            // Команда /shop_rgx_radius radius stack regex_pattern
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_rgx_radius")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(1)) // константа
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(1, 64)) // константа
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

    private static void registerSettingCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop_settings")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("default")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value", IntegerArgumentType.integer(1))
                                                    .executes(context -> {
                                                        int value = IntegerArgumentType.getInteger(context, "value");
                                                        Config config = getConfig();
                                                        config.default_radius = value;
                                                        setConfig(config);
                                                        context.getSource().sendFeedback(Text.literal("§aУстановлен радиус по умолчанию: §e" + value));
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("stack")
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 64))
                                                    .executes(context -> {
                                                        int value = IntegerArgumentType.getInteger(context, "value");
                                                        Config config = getConfig();
                                                        config.default_stack = value;
                                                        setConfig(config);
                                                        context.getSource().sendFeedback(Text.literal("§aУстановлен стак по умолчанию: §e" + value));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("price_pattern")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String value = StringArgumentType.getString(context, "value");
                                                Config config = getConfig();
                                                config.price_pattern = value;
                                                setConfig(config);
                                                context.getSource().sendFeedback(Text.literal("§aУстановлен паттерн цены: §e" + value));
                                                return 1;
                                            })
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("amount_pattern")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String value = StringArgumentType.getString(context, "value");
                                                Config config = getConfig();
                                                config.amount_pattern = value;
                                                setConfig(config);
                                                context.getSource().sendFeedback(Text.literal("§aУстановлен паттерн количества: §e" + value));
                                                return 1;
                                            })
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("y_coords")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("values", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String valuesStr = StringArgumentType.getString(context, "values");
                                                try {
                                                    String[] parts = valuesStr.split("\\s+");
                                                    List<Integer> yCoords = new ArrayList<>();
                                                    for (String part : parts) {
                                                        yCoords.add(Integer.parseInt(part));
                                                    }

                                                    Config config = getConfig();
                                                    config.y_coords = yCoords;
                                                    setConfig(config);
                                                    context.getSource().sendFeedback(Text.literal("§aУстановлены Y координаты: §e" + yCoords));
                                                } catch (NumberFormatException e) {
                                                    context.getSource().sendFeedback(Text.literal("§cОшибка: неверный формат чисел"));
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("highlight_color")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("color1", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                // Саджесты для первого цвета
                                                for (Formatting formatting : Formatting.values()) {
                                                    if (formatting.isColor()) {
                                                        builder.suggest(formatting.getName());
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("color2", StringArgumentType.string())
                                                    .suggests((context, builder) -> {
                                                        // Саджесты для второго цвета
                                                        for (Formatting formatting : Formatting.values()) {
                                                            if (formatting.isColor()) {
                                                                builder.suggest(formatting.getName());
                                                            }
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(context -> {
                                                        String color1Name = StringArgumentType.getString(context, "color1").toLowerCase();
                                                        String color2Name = StringArgumentType.getString(context, "color2").toLowerCase();

                                                        // Валидируем цвета
                                                        Formatting color1 = null;
                                                        Formatting color2 = null;

                                                        for (Formatting formatting : Formatting.values()) {
                                                            if (formatting.isColor() && formatting.getName().equalsIgnoreCase(color1Name)) {
                                                                color1 = formatting;
                                                            }
                                                            if (formatting.isColor() && formatting.getName().equalsIgnoreCase(color2Name)) {
                                                                color2 = formatting;
                                                            }
                                                        }

                                                        if (color1 != null && color2 != null) {
                                                            Config config = getConfig();
                                                            config.highlight_colors = Arrays.asList(color1.getName(), color2.getName());
                                                            setConfig(config);

                                                            context.getSource().sendFeedback(Text.literal("§aУстановлены цвета подсветки: ")
                                                                    .append(Text.literal(color1.getName()).formatted(color1))
                                                                    .append(Text.literal(" §f(Цвет 1) и "))
                                                                    .append(Text.literal(color2.getName()).formatted(color2))
                                                                    .append(Text.literal(" §f(Цвет 2)")));
                                                        } else {
                                                            // Показываем доступные цвета в ошибке
                                                            StringBuilder availableColors = new StringBuilder();
                                                            for (Formatting formatting : Formatting.values()) {
                                                                if (formatting.isColor()) {
                                                                    if (availableColors.length() > 0) availableColors.append(", ");
                                                                    availableColors.append(formatting.getName());
                                                                }
                                                            }

                                                            context.getSource().sendFeedback(Text.literal("§cОшибка: неверные цвета. Допустимые значения: §e" + availableColors));
                                                        }
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("use_navigation_modifiers")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                                            .executes(context -> {
                                                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                                Config config = getConfig();
                                                config.use_navigation_modifiers = enabled;
                                                setConfig(config);

                                                String status = enabled ? "§aвключены" : "§cотключены";
                                                context.getSource().sendFeedback(Text.literal("§aМодификаторы навигации: " + status));
                                                return 1;
                                            })
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("use_quick_shop_modifier")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                                            .executes(context -> {
                                                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                                Config config = getConfig();
                                                config.use_quick_shop_modifier = enabled;
                                                setConfig(config);

                                                String status = enabled ? "§aвключен" : "§cотключен";
                                                context.getSource().sendFeedback(Text.literal("§aМодификатор быстрого магазина: " + status));
                                                return 1;
                                            })
                                    )
                            )
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("show")
                                    .executes(context -> {
                                        Config config = getConfig();
                                        Formatting color1 = convertColorNameToFormatting(config.highlight_colors.get(0));
                                        Formatting color2 = convertColorNameToFormatting(config.highlight_colors.get(1));

                                        context.getSource().sendFeedback(Text.literal("§6=== НАСТРОЙКИ PEPELANDSHOP ==="));
                                        context.getSource().sendFeedback(Text.literal("§aРадиус по умолчанию: §e" + config.default_radius));
                                        context.getSource().sendFeedback(Text.literal("§aСтак по умолчанию: §e" + config.default_stack));
                                        context.getSource().sendFeedback(Text.literal("§aПаттерн цены: §e" + config.price_pattern));
                                        context.getSource().sendFeedback(Text.literal("§aПаттерн количества: §e" + config.amount_pattern));
                                        context.getSource().sendFeedback(Text.literal("§aY координаты: §e" + config.y_coords));
                                        context.getSource().sendFeedback(Text.literal("§aЦвета подсветки: ")
                                                .append(Text.literal(config.highlight_colors.get(0)).formatted(color1))
                                                .append(Text.literal(" §fи "))
                                                .append(Text.literal(config.highlight_colors.get(1)).formatted(color2)));
                                        context.getSource().sendFeedback(Text.literal("§aМодификаторы навигации: §e" + (config.use_navigation_modifiers ? "включены" : "отключены")));
                                        context.getSource().sendFeedback(Text.literal("§aМодификатор быстрого магазина: §e" + (config.use_quick_shop_modifier ? "включен" : "отключен")));
                                        context.getSource().sendFeedback(Text.literal("§7Настройте клавиши и модификаторы в меню §fControls§7!"));
                                        return 1;
                                    })
                            )
                        );
        });
    }

    private static DyeColor formattingToDyeColor(Formatting formatting) {
        if (formatting == null) return DyeColor.LIME;

        switch (formatting) {
            case WHITE: return DyeColor.WHITE;
            case YELLOW: return DyeColor.YELLOW;
            case LIGHT_PURPLE: return DyeColor.MAGENTA;
            case RED: return DyeColor.RED;
            case AQUA: return DyeColor.CYAN;
            case GREEN: return DyeColor.GREEN;
            case BLUE: return DyeColor.BLUE;
            case DARK_GRAY: return DyeColor.GRAY;
            case GRAY: return DyeColor.LIGHT_GRAY;
            case GOLD: return DyeColor.ORANGE;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case DARK_RED: return DyeColor.BROWN;
            case DARK_AQUA: return DyeColor.CYAN;
            case DARK_GREEN: return DyeColor.GREEN;
            case DARK_BLUE: return DyeColor.BLUE;
            case BLACK: return DyeColor.BLACK;
            default: return DyeColor.LIME;
        }
    }

    private static void handleKeyNavigation(net.minecraft.client.MinecraftClient client) {
        if (!navigationActive) return;

        // Проверяем назначен ли модификатор навигации
        boolean hasNavModifier = PepelandShopKeyBinds.NAVIGATION_MODIFIER != null && !PepelandShopKeyBinds.NAVIGATION_MODIFIER.isUnbound();
        boolean navModifierPressed = hasNavModifier && PepelandShopKeyBinds.NAVIGATION_MODIFIER.isPressed();

        // Следующая группа
        boolean nextReady = hasNavModifier ?
                (navModifierPressed && PepelandShopKeyBinds.NEXT_GROUP.isPressed()) : // С модификатором
                PepelandShopKeyBinds.NEXT_GROUP.isPressed(); // Без модификатора

        if (nextReady && !wasNextPressed) {
            wasNextPressed = true;
            nextGroup();
        } else if (!PepelandShopKeyBinds.NEXT_GROUP.isPressed()) {
            wasNextPressed = false;
        }

        // Предыдущая группа
        boolean prevReady = hasNavModifier ?
                (navModifierPressed && PepelandShopKeyBinds.PREVIOUS_GROUP.isPressed()) : // С модификатором
                PepelandShopKeyBinds.PREVIOUS_GROUP.isPressed(); // Без модификатора

        if (prevReady && !wasPrevPressed) {
            wasPrevPressed = true;
            previousGroup();
        } else if (!PepelandShopKeyBinds.PREVIOUS_GROUP.isPressed()) {
            wasPrevPressed = false;
        }

        // Остановка навигации - БЕЗ МОДИФИКАТОРА (всегда)
        if (PepelandShopKeyBinds.END_NAVIGATION.isPressed() && !wasStopPressed) {
            wasStopPressed = true;
            stopNavigation();
        } else if (!PepelandShopKeyBinds.END_NAVIGATION.isPressed()) {
            wasStopPressed = false;
        }
    }

    // Обновленный метод для проверки KeyBind + модификатор
    private static boolean isKeyBindPressed(KeyBinding mainKey, KeyBinding modifier) {
        if (mainKey == null || mainKey.isUnbound()) {
            return false;
        }

        // Если модификатор не указан, проверяем только основную клавишу
        if (modifier == null || modifier.isUnbound()) {
            return mainKey.isPressed();
        }

        return modifier.isPressed() && mainKey.isPressed();
    }

    private static void handleQuickShop(net.minecraft.client.MinecraftClient client) {
        if (PepelandShopKeyBinds.QUICK_SHOP == null || PepelandShopKeyBinds.QUICK_SHOP.isUnbound()) return;

        Config config = getConfig();

        // Проверяем назначен ли модификатор быстрого магазина
        boolean hasQuickShopModifier = PepelandShopKeyBinds.QUICK_SHOP_MODIFIER != null && !PepelandShopKeyBinds.QUICK_SHOP_MODIFIER.isUnbound();
        boolean quickShopModifierPressed = hasQuickShopModifier && PepelandShopKeyBinds.QUICK_SHOP_MODIFIER.isPressed();

        // Если модификатор назначен - проверяем use_quick_shop_modifier
        // Если модификатор не назначен - всегда работаем
        boolean modifierReady = hasQuickShopModifier ?
                (!config.use_quick_shop_modifier || quickShopModifierPressed) : // С проверкой настройки
                true; // Без модификатора - всегда готов

        if (modifierReady && PepelandShopKeyBinds.QUICK_SHOP.wasPressed()) {
            // Открываем чат с командой магазина
            openChatWithText("/shop 64 ");
        }
    }

    private static void openChatWithText(String text) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

        if (client.player != null) {
            // Открываем чат с заранее введенным текстом
            client.setScreen(new net.minecraft.client.gui.screen.ChatScreen(text));
        }
    }

    private static void startNavigation(Map<Double, List<BlockPos>> sortedSigns, int stack) {
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

        // Конвертируем Formatting в DyeColor для BlockHighlighter
        DyeColor firstColor = formattingToDyeColor(getFirstHighlightColor());
        DyeColor secondColor = formattingToDyeColor(getSecondHighlightColor());

        BlockHighlighter.highlightBlocks(currentGroup, firstColor, secondColor);

        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            String categoryMessage = "§aКатегория: §e" + parseMessage(currentPrice, currentStackSize);
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                    Text.literal(categoryMessage),
                    true
            );
        }
    }

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
            source.sendFeedback(Text.literal("§aRegex патерн: §f" + pattern));
        } else {
            source.sendFeedback(Text.literal("§aПатерн поиска: §f" + pattern));
        }
        source.sendFeedback(Text.literal("§aРазмер стака: §f" + stack));
        source.sendFeedback(Text.literal("§aРадиус: §f" + radius));

        List<BlockPos> foundSigns = findSignsAroundPlayer(source, radius, pattern, useRegex);

        source.sendFeedback(Text.literal("§6Найдено табличек: §e" + foundSigns.size()));

        World world = source.getWorld();
        Map<Double, List<BlockPos>> sortedSigns = sortAndGroupSignsByPrice(foundSigns, stack, world);

        // Выводим инструкции по навигации в чат
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§6=== НАВИГАЦИЯ ==="));

        // Проверяем назначены ли модификаторы
        boolean hasNavModifier = PepelandShopKeyBinds.NAVIGATION_MODIFIER != null && !PepelandShopKeyBinds.NAVIGATION_MODIFIER.isUnbound();
        boolean hasQuickShopModifier = PepelandShopKeyBinds.QUICK_SHOP_MODIFIER != null && !PepelandShopKeyBinds.QUICK_SHOP_MODIFIER.isUnbound();
        Config config = getConfig();

        // Навигация
        if (hasNavModifier) {
            // С модификатором
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.NAVIGATION_MODIFIER) + " + " + getKeyBindDisplay(PepelandShopKeyBinds.NEXT_GROUP) + " §7- следующая группа"));
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.NAVIGATION_MODIFIER) + " + " + getKeyBindDisplay(PepelandShopKeyBinds.PREVIOUS_GROUP) + " §7- предыдущая группа"));
        } else {
            // Без модификатора
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.NEXT_GROUP) + " §7- следующая группа"));
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.PREVIOUS_GROUP) + " §7- предыдущая группа"));
        }

        // Остановка навигации - всегда без модификатора
        source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.END_NAVIGATION) + " §7- завершить навигацию"));

        // Быстрый магазин
        if (hasQuickShopModifier && config.use_quick_shop_modifier) {
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.QUICK_SHOP_MODIFIER) + " + " + getKeyBindDisplay(PepelandShopKeyBinds.QUICK_SHOP) + " §7- быстрый магазин"));
        } else if (hasQuickShopModifier && !config.use_quick_shop_modifier) {
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.QUICK_SHOP) + " §7- быстрый магазин §7(модификатор отключен)"));
        } else {
            source.sendFeedback(Text.literal("§e" + getKeyBindDisplay(PepelandShopKeyBinds.QUICK_SHOP) + " §7- быстрый магазин"));
        }

        source.sendFeedback(Text.literal("§6=================="));

        if (!sortedSigns.isEmpty()) {
            startNavigation(sortedSigns, stack);
        } else {
            source.sendFeedback(Text.literal("§cСовпадения не найдены!"));
        }

        return 1;
    }

    private static List<BlockPos> findSignsAroundPlayer(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            int radius, String pattern, boolean useRegex) {
        List<BlockPos> foundSigns = new ArrayList<>();
        World world = source.getWorld();
        BlockPos playerPos = source.getPlayer().getBlockPos();

        int minX = playerPos.getX() - radius;
        int maxX = playerPos.getX() + radius;
        int minZ = playerPos.getZ() - radius;
        int maxZ = playerPos.getZ() + radius;

        // Используем настраиваемые Y координаты из конфига
        int minY = getMinYSearch();
        int maxY = getMaxYSearch();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);

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

    private static double parsePricePerUnit(SignBlockEntity sign, int stackAmount) {
        String[] lines = getFrontTextArray(sign);
        String allLinesLower = String.join(" ", lines).toLowerCase();

        double resultPrice = 9999.0;
        int resultAmount = 1;

        if (allLinesLower.contains("бесплат")) {
            resultPrice = -1;
        } else {
            // Используем паттерн цены из конфига
            List<String> foundPricesPatterns = regexFindAll(allLinesLower, getPricePattern());
            int countPrices = foundPricesPatterns.size();

            if (countPrices == 1) {
                String foundPriceStr = foundPricesPatterns.get(0);
                int price = parsePrice(foundPriceStr);
                resultPrice = price;

                String textWithoutPrice = allLinesLower.replace(foundPriceStr, "");
                // Используем паттерн количества из конфига
                List<String> foundAmountPatterns = regexFindAll(textWithoutPrice, getAmountPattern());

                if (!foundAmountPatterns.isEmpty()) {
                    int amount = parseAmount(foundAmountPatterns.get(0), stackAmount);
                    resultAmount = amount;
                }

            } else if (countPrices == 2) {
                // ... остальная логика без изменений, но использует getAmountPattern()
                List<String> foundAmountPatterns = regexFindAll(allLinesLower, getAmountPattern());
                // ... остальной код
            }
        }

        if (resultAmount == 0) {
            resultAmount = 1;
        }
        return (double) resultPrice / resultAmount;
    }

    private static String[] getFrontTextArray(SignBlockEntity sign) {
        String[] text = new String[4];
        for (int i = 0; i < 4; i++) {
            Text line = sign.getText(true).getMessage(i, false);
            text[i] = line.getString().trim();
        }
        return text;
    }

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

    private static int parsePrice(String text) {
        String digits = text.replaceAll("\\D", "");
        int price = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        String modifierWord = text.replace(digits, "").replace(" ", "").toLowerCase();

        if (modifierWord.startsWith("аб")) {
            price = price * 9;
        }
        return price;
    }

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
                message.append("§d").append(shulkers).append("шалк§r");
                f = true;
            }

            // Стаки - голубой
            if (stacks >= 1) {
                if (f) message.append("§7+§r");
                message.append("§b").append(stacks).append("ст§r");
                f = true;
            }

            // Штуки - зеленый
            if (amountInt >= 1) {
                if (f) message.append("§7+§r");
                message.append("§a").append(amountInt).append("шт§r");
            }
        }

        return message.toString();
    }

    public static int getDefaultSearchRadius() {
        return getConfig().default_radius;
    }

    public static int getMinYSearch() {
        List<Integer> yCoords = getConfig().y_coords;
        return yCoords.size() > 0 ? yCoords.get(0) : 0;
    }

    public static int getMaxYSearch() {
        List<Integer> yCoords = getConfig().y_coords;
        return yCoords.size() > 1 ? yCoords.get(1) : 3;
    }

    public static String getPricePattern() {
        return getConfig().price_pattern;
    }

    public static String getAmountPattern() {
        return getConfig().amount_pattern;
    }

    public static Formatting getFirstHighlightColor() {
        List<String> colors = getConfig().highlight_colors;
        String firstColor = colors.size() > 0 ? colors.get(0) : "white";
        return convertColorNameToFormatting(firstColor);
    }

    public static Formatting getSecondHighlightColor() {
        List<String> colors = getConfig().highlight_colors;
        String secondColor = colors.size() > 1 ? colors.get(1) : "black";
        return convertColorNameToFormatting(secondColor);
    }

    private static Formatting convertColorNameToFormatting(String colorName) {
        try {
            return Formatting.byName(colorName.toUpperCase());
        } catch (Exception e) {
            return Formatting.GREEN; // по умолчанию
        }
    }

    private static String getKeyBindDisplay(KeyBinding keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) {
            return "§cНе назначена";
        }
        return "§e" + keyBinding.getBoundKeyLocalizedText().getString();
    }
}