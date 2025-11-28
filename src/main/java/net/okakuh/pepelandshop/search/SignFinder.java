package net.okakuh.pepelandshop.search;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.okakuh.pepelandshop.managers.ConfigManager;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SignFinder {

    public static List<BlockPos> findSignsAroundPlayer(FabricClientCommandSource source,
                                                       int radius, String pattern, boolean useRegex) {
        List<BlockPos> foundSigns = new ArrayList<>();
        World world = source.getWorld();
        BlockPos playerPos = source.getPlayer().getBlockPos();

        int minX = playerPos.getX() - radius;
        int maxX = playerPos.getX() + radius;
        int minZ = playerPos.getZ() - radius;
        int maxZ = playerPos.getZ() + radius;

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

    public static Map<Double, List<BlockPos>> sortAndGroupSignsByPrice(List<BlockPos> foundSigns, int stackAmount, World world) {
        List<SignPriceInfo> signPriceInfos = new ArrayList<>();

        for (BlockPos pos : foundSigns) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SignBlockEntity sign) {
                String[] signLines = getFrontTextArray(sign);
                SignParser.ParsedSignInfo parsedInfo = SignParser.parseSign(
                        signLines,
                        stackAmount,
                        getPricePattern(),
                        getAmountPattern()
                );
                double pricePerUnit = parsedInfo.pricePerUnit;
                signPriceInfos.add(new SignPriceInfo(pos, pricePerUnit));
            }
        }

        // Сортируем по цене за единицу
        signPriceInfos.sort((a, b) -> Double.compare(a.pricePerUnit, b.pricePerUnit));

        // Группируем по одинаковым ценам
        Map<Double, List<BlockPos>> groupedSigns = new LinkedHashMap<>();
        for (SignPriceInfo info : signPriceInfos) {
            groupedSigns.computeIfAbsent(info.pricePerUnit, k -> new ArrayList<>()).add(info.position);
        }

        return groupedSigns;
    }

    private static boolean signMatchesSearch(SignBlockEntity sign, String pattern, boolean useRegex) {
        String signText = getFrontText(sign);

        if (useRegex) {
            return matchesRegex(signText, pattern);
        } else {
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
            Text line = sign.getText(true).getMessage(i, false);
            text[i] = line.getString().trim();
        }
        return String.join(" ", text);
    }

    private static String[] getFrontTextArray(SignBlockEntity sign) {
        String[] text = new String[4];
        for (int i = 0; i < 4; i++) {
            Text line = sign.getText(true).getMessage(i, false);
            text[i] = line.getString().trim();
        }
        return text;
    }

    private static boolean evaluateSearchPattern(String text, String pattern) {
        if (!pattern.contains("+") && !pattern.contains("-")) {
            return text.contains(pattern);
        }

        String[] orGroups = pattern.split("-");
        for (String orGroup : orGroups) {
            if (evaluateAndGroup(text, orGroup.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean evaluateAndGroup(String text, String andGroup) {
        if (!andGroup.contains("+")) {
            return text.contains(andGroup.trim());
        }

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

    private static int getMinYSearch() {
        return ConfigManager.getMinY();
    }

    private static int getMaxYSearch() {
        return ConfigManager.getMaxY();
    }

    private static String getPricePattern() {
        return ConfigManager.getPricePattern();
    }

    private static String getAmountPattern() {
        return ConfigManager.getAmountPattern();
    }

    private static class SignPriceInfo {
        public BlockPos position;
        public double pricePerUnit;

        public SignPriceInfo(BlockPos position, double pricePerUnit) {
            this.position = position;
            this.pricePerUnit = pricePerUnit;
        }
    }
}