// ConfigManager.java
package net.okakuh.pplshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pplshop.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config = new Config();

    public static class Config {
        public int default_radius = 40;
        public int default_stack = 64;
        public String price_pattern = "\\d+\\s*а[а-яё]{1}";
        public String amount_pattern = "\\d+\\s*[а-яё]{2}";
        public List<Integer> y_coords = Arrays.asList(0, 3);
        public String highlight_color = "LIME";
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
            System.err.println("Failed to load PPLShop config: " + e.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            System.err.println("Failed to save PPLShop config: " + e.getMessage());
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