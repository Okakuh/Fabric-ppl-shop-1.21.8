package net.okakuh.pepelandshop.search;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

public class SignParser {

    public static class ParsedSignInfo {
        public final double pricePerUnit;
        public final int originalPrice;
        public final int amount;

        public ParsedSignInfo(double pricePerUnit, int originalPrice, int amount) {
            this.pricePerUnit = pricePerUnit;
            this.originalPrice = originalPrice;
            this.amount = amount;
        }
    }

    public static ParsedSignInfo parseSign(String[] signLines, int stackAmount, String pricePattern, String amountPattern) {
        String allLinesLower = String.join(" ", signLines).toLowerCase();

        double resultPrice = 9999.0;
        int resultAmount = 1;
        int originalPrice = 9999;

        if (allLinesLower.contains("бесплат")) {
            resultPrice = -1;
        } else {
            List<String> foundPricesPatterns = regexFindAll(allLinesLower, pricePattern);
            int countPrices = foundPricesPatterns.size();

            if (countPrices == 1) {
                String foundPriceStr = foundPricesPatterns.get(0);
                int price = parsePriceValue(foundPriceStr);
                resultPrice = price;

                String textWithoutPrice = allLinesLower.replace(foundPriceStr, "");
                List<String> foundAmountPatterns = regexFindAll(textWithoutPrice, amountPattern);

                if (!foundAmountPatterns.isEmpty()) {
                    int amount = parseAmountValue(foundAmountPatterns.get(0), stackAmount);
                    resultAmount = amount;
                }

            } else if (countPrices == 2) {
                double lastPricePerUnit = 0;
                for (String line : signLines) {
                    List<String> linePricePatterns = regexFindAll(line.toLowerCase(), pricePattern);

                    if (!linePricePatterns.isEmpty()) {
                        String foundPriceStr = linePricePatterns.get(0);
                        int price = parsePriceValue(foundPriceStr);
                        resultPrice = price;

                        String textWithoutPrice = allLinesLower.replace(foundPriceStr, "");
                        List<String> foundAmountPatterns = regexFindAll(textWithoutPrice, amountPattern);

                        if (!foundAmountPatterns.isEmpty()) {
                            int amount = parseAmountValue(foundAmountPatterns.get(0), stackAmount);
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
                    }
                }
            }
        }

        if (resultAmount == 0) {
            resultAmount = 1;
        }

        originalPrice = (int) resultPrice;
        double finalPricePerUnit = (double) resultPrice / resultAmount;
        return new ParsedSignInfo(finalPricePerUnit, originalPrice, resultAmount);
    }

    public static int parsePriceValue(String text) {
        String digits = text.replaceAll("\\D", "");
        int price = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        String modifierWord = text.replace(digits, "").replace(" ", "").toLowerCase();

        if (modifierWord.startsWith("аб")) {
            price = price * 9;
        }
        return price;
    }

    public static int parseAmountValue(String amountText, int stackAmount) {
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

    public static String formatPriceMessage(double price, int stackAmount) {
        StringBuilder message = new StringBuilder();

        if (price == -1) {
            message.append("§aБЕСПЛАТНО§r");
        } else if (price == 9999) {
            message.append("Цена не найдена!");
        } else {
            double amount = 1;
            if (price < 1 && price != -1) {
                amount = 1 / price;
                price = 1;
            }

            int priceInt = (int) price;
            message.append("§e").append(priceInt).append("алм.§r");

            int ab = (int) Math.floor(priceInt / 9.0);
            if (ab >= 2) {
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

            boolean hasPrevious = false;

            if (shulkers >= 1) {
                message.append("§d").append(shulkers).append("шалк§r");
                hasPrevious = true;
            }

            if (stacks >= 1) {
                if (hasPrevious) message.append("§7+§r");
                message.append("§b").append(stacks).append("ст§r");
                hasPrevious = true;
            }

            if (amountInt >= 1) {
                if (hasPrevious) message.append("§7+§r");
                message.append("§a").append(amountInt).append("шт§r");
            }
        }

        return message.toString();
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
}