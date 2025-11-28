package net.okakuh.pepelandshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.okakuh.pepelandshop.managers.ConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Configs {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pepelandshop.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config = new Config();

    public static class Config {
        public int default_radius = 40;
        public int default_stack = 64;

        public String price_pattern = "\\d+\\s*а[а-яё]{1}";
        public String amount_pattern = "\\d+\\s*[а-яё]{2}";

        public List<Integer> y_coords = Arrays.asList(0, 3);

        public List<String> highlight_colors = Arrays.asList("white", "black");

        public String quick_shop_message = "/shop 64 ";
        public boolean quick_shop_enabled = false;

        public KeyBindConfig group_next = new KeyBindConfig("up", null);
        public KeyBindConfig group_previous = new KeyBindConfig("down", null);
        public KeyBindConfig end_navigation = new KeyBindConfig("backspace", null);
        public KeyBindConfig quick_shop = new KeyBindConfig("s", "left_alt");
    }

    private static Config createDefaultConfig() {
        Config defaultConfig = new Config();
        return defaultConfig;
    }

    public static Config getDefaultConfig() {
        return createDefaultConfig();
    }

    public static class KeyBindConfig {
        public String main_key;
        public String modifier_key; //

        public KeyBindConfig() {
        }

        public KeyBindConfig(String main_key, String modifier_key) {
            this.main_key = main_key;
            this.modifier_key = modifier_key;
        }

        public boolean hasModifier() {
            return modifier_key != null && !modifier_key.isEmpty();
        }

        @Override
        public String toString() {
            if (hasModifier()) {
                return modifier_key + " + " + main_key;
            }
            return main_key;
        }

        public boolean equals(KeyBindConfig other) {
            if (other == null) return false;

            // Безопасная проверка main_key
            if (main_key == null && other.main_key != null) return false;
            if (main_key != null && !main_key.equals(other.main_key)) return false;

            // Безопасная проверка modifier_key
            if (modifier_key == null && other.modifier_key != null) return false;
            if (modifier_key != null && !modifier_key.equals(other.modifier_key)) return false;

            return true;
        }
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                config = GSON.fromJson(json, Config.class);
                // Валидируем конфиг после загрузки
                ConfigManager.validateAndFixConfig();
            } else {
                saveConfig();
            }
        } catch (IOException e) {
            System.err.println("Failed to load PepelandShop config: " + e.getMessage());
            // Создаем валидный конфиг по умолчанию
            config = new Config();
            ConfigManager.validateAndFixConfig();
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            System.err.println("Failed to save PepelandShop config: " + e.getMessage());
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config newConfig) {
        config = newConfig;
        saveConfig();
    }
}