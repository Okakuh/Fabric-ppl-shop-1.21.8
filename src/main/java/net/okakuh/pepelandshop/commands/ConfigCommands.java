package net.okakuh.pepelandshop.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.okakuh.pepelandshop.managers.ConfigManager;
import net.okakuh.pepelandshop.Configs;
import net.okakuh.pepelandshop.managers.KeyBindManager;
import net.okakuh.pepelandshop.util.KeyBindings;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ConfigCommands {

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(literal("shop_config")
                    .executes(context -> {
                        showConfigHelp(context);
                        return 1;
                    })

                    // === Радиус поиска ===
                    .then(literal("radius")
                            .executes(context -> {
                                showCurrentRadius(context);
                                return 1;
                            })
                            .then(argument("value", IntegerArgumentType.integer(1, 1000))
                                    .executes(context -> {
                                        int radius = IntegerArgumentType.getInteger(context, "value");
                                        return setRadius(radius, context);
                                    })
                            )
                    )

                    // === Размер стака ===
                    .then(literal("stack")
                            .executes(context -> {
                                showCurrentStackSize(context);
                                return 1;
                            })
                            .then(argument("value", IntegerArgumentType.integer(1, 64))
                                    .executes(context -> {
                                        int stack = IntegerArgumentType.getInteger(context, "value");
                                        return setStackSize(stack, context);
                                    })
                            )
                    )

                    // === Y координаты ===
                    .then(literal("y_coords")
                            .executes(context -> {
                                showCurrentYCoords(context);
                                return 1;
                            })
                            .then(literal("min")
                                    .then(argument("value", IntegerArgumentType.integer(-64, 319))
                                            .executes(context -> {
                                                int minY = IntegerArgumentType.getInteger(context, "value");
                                                return setMinY(minY, context);
                                            })
                                    )
                            )
                            .then(literal("max")
                                    .then(argument("value", IntegerArgumentType.integer(-64, 319))
                                            .executes(context -> {
                                                int maxY = IntegerArgumentType.getInteger(context, "value");
                                                return setMaxY(maxY, context);
                                            })
                                    )
                            )
                            .then(literal("set")
                                    .then(argument("min", IntegerArgumentType.integer(-64, 319))
                                            .then(argument("max", IntegerArgumentType.integer(-64, 319))
                                                    .executes(context -> {
                                                        int minY = IntegerArgumentType.getInteger(context, "min");
                                                        int maxY = IntegerArgumentType.getInteger(context, "max");
                                                        return setYCoords(minY, maxY, context);
                                                    })
                                            )
                                    )
                            )
                    )

                    // === Цвета выделения ===
                    .then(literal("highlighting")
                            .executes(context -> {
                                showCurrentHighlightColors(context);
                                return 1;
                            })
                            .then(literal("color1")
                                    .executes(context -> {
                                        showCurrentColor1(context);
                                        return 1;
                                    })
                                    .then(argument("color", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                for (DyeColor dyeColor : DyeColor.values()) {
                                                    builder.suggest(dyeColor.name().toLowerCase());
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                String color = StringArgumentType.getString(context, "color");
                                                return setHighlightColor1(color, context);
                                            })
                                    )
                            )
                            .then(literal("color2")
                                    .executes(context -> {
                                        showCurrentColor2(context);
                                        return 1;
                                    })
                                    .then(argument("color", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                for (DyeColor dyeColor : DyeColor.values()) {
                                                    builder.suggest(dyeColor.name().toLowerCase());
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                String color = StringArgumentType.getString(context, "color");
                                                return setHighlightColor2(color, context);
                                            })
                                    )
                            )
                    )

                    // === Паттерны ===
                    .then(literal("patterns")
                            .executes(context -> {
                                showCurrentPatterns(context);
                                return 1;
                            })
                            .then(literal("price")
                                    .executes(context -> {
                                        showCurrentPricePattern(context);
                                        return 1;
                                    })
                                    .then(argument("pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String pattern = StringArgumentType.getString(context, "pattern");
                                                return setPricePattern(pattern, context);
                                            })
                                    )
                            )
                            .then(literal("amount")
                                    .executes(context -> {
                                        showCurrentAmountPattern(context);
                                        return 1;
                                    })
                                    .then(argument("pattern", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String pattern = StringArgumentType.getString(context, "pattern");
                                                return setAmountPattern(pattern, context);
                                            })
                                    )
                            )
                    )

                    // === НАСТРОЙКА КЛАВИШ === (НОВАЯ СЕКЦИЯ)
                    .then(literal("keybinds")
                            .executes(context -> {
                                showKeyBindsHelp(context);
                                return 1;
                            })
                            .then(literal("show")
                                    .executes(context -> {
                                        return showCurrentKeyBinds(context);
                                    })
                            )
                            .then(literal("set")
                                    .then(literal("group_next")
                                            .executes(context -> {
                                                return startKeyBindRecording(context, "group_next", "следующая группа");
                                            })
                                    )
                                    .then(literal("group_previous")
                                            .executes(context -> {
                                                return startKeyBindRecording(context, "group_previous", "предыдущая группа");
                                            })
                                    )
                                    .then(literal("end_navigation")
                                            .executes(context -> {
                                                return startKeyBindRecording(context, "end_navigation", "завершить навигацию");
                                            })
                                    )
                                    .then(literal("quick_shop")
                                            .executes(context -> {
                                                return startKeyBindRecording(context, "quick_shop", "быстрый магазин");
                                            })
                                    )
                            )
                            .then(literal("reset")
                                    .executes(context -> {
                                        return resetKeyBinds(context);
                                    })
                            )
                    )
                    .then(literal("quick_shop")
                            .then(literal("message")
                                    .executes(context -> {
                                        showCurrentQuickShopMessage(context);
                                        return 1;
                                    })
                                    .then(argument("message", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String message = StringArgumentType.getString(context, "message");
                                                return setQuickShopMessage(message, context);
                                            })
                                    )
                            )
                            .then(literal("enabled")
                                    .executes(context -> {
                                        showCurrentQuickShopEnabled(context);
                                        return 1;
                                    })
                                    .then(argument("state", BoolArgumentType.bool())
                                            .executes(context -> {
                                                boolean enabled = BoolArgumentType.getBool(context, "state");
                                                return setQuickShopEnabled(enabled, context);
                                            })
                                    )
                            )
                    )
                    // === Сброс настроек ===
                    .then(literal("reset")
                            .executes(context -> {
                                return resetConfig(context);
                            })
                    )

                    // === Показать все настройки ===
                    .then(literal("show")
                            .executes(context -> {
                                return showAllSettings(context);
                            })
                    )
            );
        });
    }

    private static void showCurrentQuickShopMessage(CommandContext<FabricClientCommandSource> context) {
        String currentMessage = ConfigManager.getQuickShopMessage();
        context.getSource().sendFeedback(Text.literal("§aТекущее сообщение быстрого магазина: §e" + currentMessage));
    }

    private static int setQuickShopMessage(String message, CommandContext<FabricClientCommandSource> context) {
        ConfigManager.setQuickShopMessage(message);
        context.getSource().sendFeedback(Text.literal("§aСообщение быстрого магазина установлено на: §e" + message));
        return 1;
    }

    private static void showCurrentQuickShopEnabled(CommandContext<FabricClientCommandSource> context) {
        boolean enabled = ConfigManager.isQuickShopEnabled();
        String status = enabled ? "§aВключен" : "§cВыключен";
        context.getSource().sendFeedback(Text.literal("§aБыстрый магазин: " + status));
    }

    private static int setQuickShopEnabled(boolean enabled, CommandContext<FabricClientCommandSource> context) {
        ConfigManager.setQuickShopEnabled(enabled);
        String status = enabled ? "§aвключен" : "§cвыключен";
        context.getSource().sendFeedback(Text.literal("§aБыстрый магазин " + status));
        return 1;
    }

    private static void showConfigHelp(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        source.sendFeedback(Text.literal("§6=== PepelandShop Configuration ==="));
        source.sendFeedback(Text.literal("§e/shop_config show §7- показать все текущие настройки"));
        source.sendFeedback(Text.literal("§e/shop_config radius [значение] §7- радиус поиска (1-1000)"));
        source.sendFeedback(Text.literal("§e/shop_config stack [значение] §7- размер стака (1-64)"));
        source.sendFeedback(Text.literal("§e/shop_config y_coords §7- работа с Y координатами"));
        source.sendFeedback(Text.literal("§e/shop_config highlighting §7- настройка цветов выделения"));
        source.sendFeedback(Text.literal("§e/shop_config patterns §7- настройка паттернов поиска"));
        source.sendFeedback(Text.literal("§e/shop_config keybinds §7- настройка клавиш управления"));
        source.sendFeedback(Text.literal("§aБыстрый магазин: " +
                (ConfigManager.isQuickShopEnabled() ? "§aВключен" : "§cВыключен")));
        source.sendFeedback(Text.literal("§aСообщение быстрого магазина: §e" + ConfigManager.getQuickShopMessage()));
        source.sendFeedback(Text.literal("§e/shop_config reset §7- сбросить настройки по умолчанию"));


        // Показываем доступные цвета
        StringBuilder colorList = new StringBuilder("§6Доступные цвета: ");
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            colorList.append(colors[i].name().toLowerCase());
            if (i < colors.length - 1) {
                colorList.append(", ");
            }
        }
        source.sendFeedback(Text.literal(colorList.toString()));
        source.sendFeedback(Text.literal("§6=================================="));
    }

    private static int showAllSettings(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        source.sendFeedback(Text.literal("§6=== Текущие настройки PepelandShop ==="));

        // Радиус и стак
        source.sendFeedback(Text.literal("§aРадиус поиска: §e" + ConfigManager.getSearchRadius()));
        source.sendFeedback(Text.literal("§aРазмер стака: §e" + ConfigManager.getStackSize()));

        // Y координаты
        source.sendFeedback(Text.literal("§aY координаты: §e" + ConfigManager.getMinY() + " §7- §e" + ConfigManager.getMaxY()));

        // Цвета - теперь методы работают
        source.sendFeedback(Text.literal("§aЦвета выделения: §e" +
                ConfigManager.getFirstHighlightColor().getName() + " §7и §e" +
                ConfigManager.getSecondHighlightColor().getName()));

        // Паттерны
        source.sendFeedback(Text.literal("§aПаттерн цен: §e" + ConfigManager.getPricePattern()));
        source.sendFeedback(Text.literal("§aПаттерн количеств: §e" + ConfigManager.getAmountPattern()));

        // Клавиши
        source.sendFeedback(Text.literal("§aНастройки клавиш: §eиспользуйте /shop_config keybinds show"));

        // сообщение быстрого магазина
        source.sendFeedback(Text.literal("§aБыстрый магазин: " +
                (ConfigManager.isQuickShopEnabled() ? "§aВключен" : "§cВыключен")));
        source.sendFeedback(Text.literal("§aСообщение быстрого магазина: §e" + ConfigManager.getQuickShopMessage()));

        source.sendFeedback(Text.literal("§6========================================="));
        return 1;
    }

    // === МЕТОДЫ ДЛЯ КЛАВИШ (перенесены из KeyBindsCommands) ===

    private static void showKeyBindsHelp(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        source.sendFeedback(Text.literal("§6=== Настройка клавиш PepelandShop ==="));
        source.sendFeedback(Text.literal("§e/shop_config keybinds show §7- показать текущие настройки"));
        source.sendFeedback(Text.literal("§e/shop_config keybinds set <функция> §7- установить клавишу для функции"));
        source.sendFeedback(Text.literal("§e/shop_config keybinds reset §7- сбросить клавиши к значениям по умолчанию"));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§6Доступные функции:"));
        source.sendFeedback(Text.literal("§e- group_next §7- следующая группа"));
        source.sendFeedback(Text.literal("§e- group_previous §7- предыдущая группа"));
        source.sendFeedback(Text.literal("§e- end_navigation §7- завершить навигацию"));
        source.sendFeedback(Text.literal("§e- quick_shop §7- быстрый магазин"));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§6Инструкция:"));
        source.sendFeedback(Text.literal("§7- Нажмите кнопку 1"));
        source.sendFeedback(Text.literal("§7- Нажмите §eSPACE§7 для установки одной кнопки"));
        source.sendFeedback(Text.literal("§7- Или нажмите §eкнопку 2§7 для комбинации"));
        source.sendFeedback(Text.literal("§7- Повторные нажатия той же кнопки игнорируются"));
        source.sendFeedback(Text.literal("§6====================================="));
    }

    private static int showCurrentKeyBinds(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        source.sendFeedback(Text.literal("§6=== Текущие настройки клавиш ==="));

        var config = Configs.getConfig();
        source.sendFeedback(Text.literal("§aСледующая группа: §e" + config.group_next));
        source.sendFeedback(Text.literal("§aПредыдущая группа: §e" + config.group_previous));
        source.sendFeedback(Text.literal("§aЗавершить навигацию: §e" + config.end_navigation));
        source.sendFeedback(Text.literal("§aБыстрый магазин: §e" + config.quick_shop));

        source.sendFeedback(Text.literal("§6================================="));
        return 1;
    }

    private static int startKeyBindRecording(CommandContext<FabricClientCommandSource> context, String keyBindId, String functionName) {
        if (KeyBindManager.isRecording()) {
            context.getSource().sendFeedback(Text.literal("§cУже идет запись другой клавиши!"));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("§6Настройка клавиши для: §e" + functionName));
        // УБИРАЕМ сообщение о задержке
        context.getSource().sendFeedback(Text.literal("§7Нажмите первую кнопку..."));

        KeyBindManager.startRecording(keyBindId, new KeyBindManager.KeyBindRecorderCallback() {
            @Override
            public void onKey1Recorded(String keyName) {
                context.getSource().sendFeedback(Text.literal("§aКнопка 1: §e" + KeyBindings.getDisplayName(keyName)));
                context.getSource().sendFeedback(Text.literal("§7Нажмите §eSPACE§7 для завершения, или кнопку 2 для комбинации"));
            }

            @Override
            public void onWaitingForKey2() {
                // Этот метод вызывается автоматически после onKey1Recorded
                // Можно оставить пустым или добавить дополнительное сообщение
            }

            @Override
            public void onRecordingCompleted(Configs.KeyBindConfig newConfig) {
                String functionName = getFunctionName(keyBindId);
                context.getSource().sendFeedback(Text.literal("§aНазначен новый ключ: §e" + newConfig + " §aдля функции: §e" + functionName));
                KeyBindManager.stopRecording();
            }

            @Override
            public void onRecordingFailed(String error) {
                context.getSource().sendFeedback(Text.literal("§cОшибка: " + error));
                KeyBindManager.stopRecording();
            }

            @Override
            public void onRecordingCancelled() {
                context.getSource().sendFeedback(Text.literal("§6Запись отменена"));
                KeyBindManager.stopRecording();
            }
        });

        return 1;
    }

    private static String getFunctionName(String keyBindId) {
        switch (keyBindId) {
            case "group_next": return "следующая группа";
            case "group_previous": return "предыдущая группа";
            case "end_navigation": return "завершить навигацию";
            case "quick_shop": return "быстрый магазин";
            default: return "неизвестная функция";
        }
    }

    private static int resetKeyBinds(CommandContext<FabricClientCommandSource> context) {
        var config = Configs.getConfig();
        config.group_next = new Configs.KeyBindConfig("up", null);
        config.group_previous = new Configs.KeyBindConfig("down", null);
        config.end_navigation = new Configs.KeyBindConfig("backspace", null);
        config.quick_shop = new Configs.KeyBindConfig("r", null);

        Configs.saveConfig();

        context.getSource().sendFeedback(Text.literal("§aВсе клавиши сброшены к значениям по умолчанию!"));
        return 1;
    }

    // === Методы для радиуса ===

    private static void showCurrentRadius(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий радиус поиска: §e" + ConfigManager.getSearchRadius()));
    }

    private static int setRadius(int radius, CommandContext<FabricClientCommandSource> context) {
        int oldRadius = ConfigManager.getSearchRadius();
        ConfigManager.setSearchRadius(radius);
        context.getSource().sendFeedback(Text.literal("§aРадиус поиска изменен: §e" + oldRadius + " §7→ §e" + radius));
        return 1;
    }

    // === Методы для размера стака ===

    private static void showCurrentStackSize(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий размер стака: §e" + ConfigManager.getStackSize()));
    }

    private static int setStackSize(int stack, CommandContext<FabricClientCommandSource> context) {
        int oldStack = ConfigManager.getStackSize();
        ConfigManager.setStackSize(stack);
        context.getSource().sendFeedback(Text.literal("§aРазмер стака изменен: §e" + oldStack + " §7→ §e" + stack));
        return 1;
    }

    // === Методы для Y координат ===

    private static void showCurrentYCoords(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущие Y координаты: §e" + ConfigManager.getMinY() + " §7- §e" + ConfigManager.getMaxY()));
    }

    private static int setMinY(int minY, CommandContext<FabricClientCommandSource> context) {
        int currentMaxY = ConfigManager.getMaxY();
        if (minY > currentMaxY) {
            context.getSource().sendFeedback(Text.literal("§cОшибка: минимальная Y координата не может быть больше максимальной!"));
            return 0;
        }
        ConfigManager.setYCoords(minY, currentMaxY);
        context.getSource().sendFeedback(Text.literal("§aМинимальная Y координата установлена на: §e" + minY));
        return 1;
    }

    private static int setMaxY(int maxY, CommandContext<FabricClientCommandSource> context) {
        int currentMinY = ConfigManager.getMinY();
        if (maxY < currentMinY) {
            context.getSource().sendFeedback(Text.literal("§cОшибка: максимальная Y координата не может быть меньше минимальной!"));
            return 0;
        }
        ConfigManager.setYCoords(currentMinY, maxY);
        context.getSource().sendFeedback(Text.literal("§aМаксимальная Y координата установлена на: §e" + maxY));
        return 1;
    }

    private static int setYCoords(int minY, int maxY, CommandContext<FabricClientCommandSource> context) {
        if (minY > maxY) {
            context.getSource().sendFeedback(Text.literal("§cОшибка: минимальная Y координата не может быть больше максимальной!"));
            return 0;
        }
        ConfigManager.setYCoords(minY, maxY);
        context.getSource().sendFeedback(Text.literal("§aY координаты установлены: §e" + minY + " §7- §e" + maxY));
        return 1;
    }

    // === Методы для цветов выделения ===

    private static void showCurrentHighlightColors(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущие цвета выделения:"));
        context.getSource().sendFeedback(Text.literal("  §aЦвет 1: §e" + ConfigManager.getFirstHighlightColor().getName()));
        context.getSource().sendFeedback(Text.literal("  §aЦвет 2: §e" + ConfigManager.getSecondHighlightColor().getName()));
    }

    private static void showCurrentColor1(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий первый цвет: §e" + ConfigManager.getFirstHighlightColor().getName()));
    }

    private static void showCurrentColor2(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий второй цвет: §e" + ConfigManager.getSecondHighlightColor().getName()));
    }

    private static int setHighlightColor1(String color, CommandContext<FabricClientCommandSource> context) {
        try {
            DyeColor.valueOf(color.toUpperCase());
            ConfigManager.setHighlightColor1(color);
            context.getSource().sendFeedback(Text.literal("§aПервый цвет выделения установлен на: §e" + color));
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("§cОшибка: неверный цвет! Используйте цвета из списка."));
            return 0;
        }
    }

    private static int setHighlightColor2(String color, CommandContext<FabricClientCommandSource> context) {
        try {
            DyeColor.valueOf(color.toUpperCase());
            ConfigManager.setHighlightColor2(color);
            context.getSource().sendFeedback(Text.literal("§aВторой цвет выделения установлен на: §e" + color));
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("§cОшибка: неверный цвет! Используйте цвета из списка."));
            return 0;
        }
    }

    // === Методы для паттернов ===

    private static void showCurrentPatterns(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущие паттерны:"));
        context.getSource().sendFeedback(Text.literal("  §aЦены: §e" + ConfigManager.getPricePattern()));
        context.getSource().sendFeedback(Text.literal("  §aКоличества: §e" + ConfigManager.getAmountPattern()));
    }

    private static void showCurrentPricePattern(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий паттерн для цен: §e" + ConfigManager.getPricePattern()));
    }

    private static void showCurrentAmountPattern(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§aТекущий паттерн для количеств: §e" + ConfigManager.getAmountPattern()));
    }

    private static int setPricePattern(String pattern, CommandContext<FabricClientCommandSource> context) {
        ConfigManager.setPricePattern(pattern);
        context.getSource().sendFeedback(Text.literal("§aПаттерн для цен установлен на: §e" + pattern));
        return 1;
    }

    private static int setAmountPattern(String pattern, CommandContext<FabricClientCommandSource> context) {
        ConfigManager.setAmountPattern(pattern);
        context.getSource().sendFeedback(Text.literal("§aПаттерн для количеств установлен на: §e" + pattern));
        return 1;
    }

    // === Сброс настроек ===

    private static int resetConfig(CommandContext<FabricClientCommandSource> context) {
        // Создаем новый конфиг по умолчанию
        Configs.Config defaultConfig = new Configs.Config();
        Configs.setConfig(defaultConfig);
        ConfigManager.validateAndFixConfig();

        context.getSource().sendFeedback(Text.literal("§aВсе настройки сброшены к значениям по умолчанию!"));
        return 1;
    }
}