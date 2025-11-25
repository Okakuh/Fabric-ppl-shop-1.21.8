// ConfigManager.java
package net.okakuh.pepelandshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {
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

        // Новые настройки навигации
        public boolean use_alternative_navigation = false;
        public boolean use_quick_shop = false;

        // Настройки клавиш
        public KeyBind quick_shop = new KeyBind("left_shift", "q");
        public AlternativeNavigationKeys alternative_navigation = new AlternativeNavigationKeys();
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                config = GSON.fromJson(json, Config.class);
            } else {
                saveConfig();
            }
        } catch (IOException e) {
            System.err.println("Failed to load PepelandShop config: " + e.getMessage());
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
    // Класс для одной клавиши/комбинации
    public static class KeyBind {
        public String modifier = ""; // может быть пустым для одиночной клавиши
        public String key = "";

        public KeyBind() {}

        public KeyBind(String modifier, String key) {
            this.modifier = modifier;
            this.key = key;
        }

        public String getCombo() {
            if (modifier.isEmpty()) {
                return key;
            } else {
                return modifier + "+" + key;
            }
        }

        public String getDisplayName() {
            if (modifier.isEmpty()) {
                return KeyBindings.getDisplayName(key);
            } else {
                return KeyBindings.getDisplayName(modifier) + " + " + KeyBindings.getDisplayName(key);
            }
        }
    }
    // Класс для альтернативной навигации
    public static class AlternativeNavigationKeys {
        public KeyBind next_group = new KeyBind("left_alt", "w");
        public KeyBind previous_group = new KeyBind("left_alt", "s");
        public KeyBind end_navigation = new KeyBind("left_alt", "backspace");
    }
}