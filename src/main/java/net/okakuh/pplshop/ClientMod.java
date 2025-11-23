package net.okakuh.pplshop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ClientMod implements ClientModInitializer {

    // ==================== НАСТРАИВАЕМЫЕ ПЕРЕМЕННЫЕ ====================

    // Параметры по умолчанию для команды /shop
    public static final int DEFAULT_STACK_SIZE = 64;
    public static final int DEFAULT_SEARCH_RADIUS = 30;

    // Настройки поиска табличек
    public static final int MIN_Y_SEARCH = 0;      // Минимальная Y координата для поиска
    public static final int MAX_Y_SEARCH = 3;      // Максимальная Y координата для поиска

    // Настройки проверки табличек
    public static final boolean SEARCH_FRONT_SIDE = true;  // Искать на передней стороне таблички

    // Ограничения параметров
    public static final int MIN_STACK_SIZE = 1;
    public static final int MAX_STACK_SIZE = 64;
    public static final int MIN_RADIUS = 0;

    // ==================== КОНЕЦ НАСТРАИВАЕМЫХ ПЕРЕМЕННЫХ ====================

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Команда /shop для обычного поиска
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shop")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("search_pattern", StringArgumentType.string())
                                    .executes(context -> {
                                        String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                        int stack = DEFAULT_STACK_SIZE;
                                        int radius = DEFAULT_SEARCH_RADIUS;
                                        return executeShop(context, searchPattern, stack, radius, false);
                                    })
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                            .executes(context -> {
                                                String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                int radius = DEFAULT_SEARCH_RADIUS;
                                                return executeShop(context, searchPattern, stack, radius, false);
                                            })
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(MIN_RADIUS))
                                                    .executes(context -> {
                                                        String searchPattern = StringArgumentType.getString(context, "search_pattern");
                                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                                        int radius = IntegerArgumentType.getInteger(context, "radius");
                                                        return executeShop(context, searchPattern, stack, radius, false);
                                                    })
                                            )
                                    )
                            )
            );

            // Команда /shopr для поиска по регулярным выражениям
            dispatcher.register(
                    net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shopr")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("regex_pattern", StringArgumentType.string())
                                    .executes(context -> {
                                        String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                        int stack = DEFAULT_STACK_SIZE;
                                        int radius = DEFAULT_SEARCH_RADIUS;
                                        return executeShop(context, regexPattern, stack, radius, true);
                                    })
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("stack", IntegerArgumentType.integer(MIN_STACK_SIZE, MAX_STACK_SIZE))
                                            .executes(context -> {
                                                String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                                int stack = IntegerArgumentType.getInteger(context, "stack");
                                                int radius = DEFAULT_SEARCH_RADIUS;
                                                return executeShop(context, regexPattern, stack, radius, true);
                                            })
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius", IntegerArgumentType.integer(MIN_RADIUS))
                                                    .executes(context -> {
                                                        String regexPattern = StringArgumentType.getString(context, "regex_pattern");
                                                        int stack = IntegerArgumentType.getInteger(context, "stack");
                                                        int radius = IntegerArgumentType.getInteger(context, "radius");
                                                        return executeShop(context, regexPattern, stack, radius, true);
                                                    })
                                            )
                                    )
                            )
            );
        });
    }

    private static int executeShop(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, String pattern, int stack, int radius, boolean useRegex) {
        var source = context.getSource();

        // Выводим параметры команды
        if (useRegex) {
            source.sendFeedback(Text.literal("§aRegex Pattern: §f" + pattern));
        } else {
            source.sendFeedback(Text.literal("§aSearch Pattern: §f" + pattern));
        }
        source.sendFeedback(Text.literal("§aStack: §f" + stack));
        source.sendFeedback(Text.literal("§aRadius: §f" + radius));

        // Ищем таблички вокруг игрока
        List<BlockPos> foundSigns = findSignsAroundPlayer(source, radius, pattern, useRegex);

        // Выводим количество найденных табличек
        source.sendFeedback(Text.literal("§6Найдено табличек: §e" + foundSigns.size()));

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
            Text line = sign.getText(SEARCH_FRONT_SIDE).getMessage(i, false);
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
}