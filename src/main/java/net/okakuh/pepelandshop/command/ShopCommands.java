package net.okakuh.pepelandshop.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.okakuh.pepelandshop.search.SignFinder;
import net.okakuh.pepelandshop.navigation.NavigationManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ShopCommands {

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // Основная команда /shop
            dispatcher.register(literal("shop")
                    .then(argument("stack", IntegerArgumentType.integer(1, 64))
                            .then(argument("search_pattern", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                        String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                        int radius = net.okakuh.pepelandshop.config.ConfigManager.getConfig().default_radius;
                                        return executeShop(context, searchPattern, stack, radius, false);
                                    })
                            )
                    )
            );
            // Команда /shop_radius
            dispatcher.register(literal("shop_radius")
                    .then(argument("radius", IntegerArgumentType.integer(1))
                            .then(argument("stack", IntegerArgumentType.integer(1, 64))
                                    .then(argument("search_pattern", StringArgumentType.greedyString())
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
            // Команда /shop_rgx
            dispatcher.register(literal("shop_rgx")
                    .then(argument("stack", IntegerArgumentType.integer(1, 64))
                            .then(argument("regex_pattern", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                        String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                        int radius = net.okakuh.pepelandshop.config.ConfigManager.getConfig().default_radius;
                                        return executeShop(context, regexPattern, stack, radius, true);
                                    })
                            )
                    )
            );
            // Команда /shop_rgx_radius
            dispatcher.register(literal("shop_rgx_radius")
                    .then(argument("radius", IntegerArgumentType.integer(1))
                            .then(argument("stack", IntegerArgumentType.integer(1, 64))
                                    .then(argument("regex_pattern", StringArgumentType.greedyString())
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

    private static int executeShop(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context,
                                   String pattern, int stack, int radius, boolean useRegex) {
        var source = context.getSource();

        // Выводим параметры поиска
        if (useRegex) {
            source.sendFeedback(Text.literal("§aRegex патерн: §f" + pattern));
        } else {
            source.sendFeedback(Text.literal("§aПатерн поиска: §f" + pattern));
        }
        source.sendFeedback(Text.literal("§aРазмер стака: §f" + stack));
        source.sendFeedback(Text.literal("§aРадиус: §f" + radius));

        // Используем ShopFinder для поиска табличек
        var foundSigns = SignFinder.findSignsAroundPlayer(source, radius, pattern, useRegex);
        source.sendFeedback(Text.literal("§6Найдено табличек: §e" + foundSigns.size()));

        // Сортируем и группируем результаты
        var sortedSigns = SignFinder.sortAndGroupSignsByPrice(foundSigns, stack, source.getWorld());

        // Показываем инструкции по навигации
        showNavigationInstructions(source);

        if (!sortedSigns.isEmpty()) {
            NavigationManager.startNavigation(sortedSigns, stack);
        } else {
            source.sendFeedback(Text.literal("§cСовпадения не найдены!"));
        }

        return 1;
    }

    private static void showNavigationInstructions(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§6=== НАВИГАЦИЯ ==="));

        // Получаем текущие настройки клавиш из конфига
        var config = net.okakuh.pepelandshop.config.ConfigManager.getConfig();

        // Навигация - используем настройки из конфига
        source.sendFeedback(Text.literal("§e" + config.group_next + " §7- следующая группа"));
        source.sendFeedback(Text.literal("§e" + config.group_previous + " §7- предыдущая группа"));

        // Остановка навигации
        source.sendFeedback(Text.literal("§e" + config.end_navigation + " §7- завершить навигацию"));


        source.sendFeedback(Text.literal("§6=================="));
    }
}