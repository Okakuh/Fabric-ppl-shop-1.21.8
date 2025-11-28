package net.okakuh.pepelandshop.managers;

import net.okakuh.pepelandshop.Configs;
import net.okakuh.pepelandshop.util.KeyBindings;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyBindManager {
    private static KeyBindRecorder currentRecorder = null;
    private static long recordingStartTime = 0;
    private static final long INITIAL_DELAY = 300;

    // Простые методы проверки нажатий через GLFW
    public static boolean isPressed(String keyBindId) {
        Configs.KeyBindConfig config = getKeyBindConfig(keyBindId);
        if (config == null) return false;

        // Проверяем основную клавишу
        int mainKeyCode = KeyBindings.getKeyCode(config.main_key);
        boolean mainPressed = GLFW.glfwGetKey(getWindowHandle(), mainKeyCode) == GLFW.GLFW_PRESS;

        // Если есть модификатор, проверяем его тоже
        if (config.hasModifier()) {
            int modifierKeyCode = KeyBindings.getKeyCode(config.modifier_key);
            boolean modifierPressed = GLFW.glfwGetKey(getWindowHandle(), modifierKeyCode) == GLFW.GLFW_PRESS;
            return modifierPressed && mainPressed;
        }

        return mainPressed;
    }

    public static boolean wasPressed(String keyBindId) {
        // Для "wasPressed" нам нужно отслеживать состояние между кадрами
        // Пока сделаем простую проверку isPressed
        return isPressed(keyBindId);
    }

    private static long getWindowHandle() {
        return net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
    }

    // Система записи новых комбинаций
    // Система записи новых комбинаций
    public static void startRecording(String keyBindId, KeyBindRecorderCallback callback) {
        currentRecorder = new KeyBindRecorder(keyBindId, callback);
        recordingStartTime = System.currentTimeMillis();
    }

    public static void stopRecording() {
        currentRecorder = null;
        recordingStartTime = 0;
    }

    public static boolean isRecording() {
        return currentRecorder != null;
    }

    public static void handleKeyInput(int keyCode, int action) {
        if (currentRecorder != null && action == GLFW.GLFW_PRESS) {
            long currentTime = System.currentTimeMillis();

            // Проверяем начальную задержку (0.3 секунды)
            if (currentTime - recordingStartTime < INITIAL_DELAY) {
                return; // Игнорируем нажатия в течение первых 0.3 секунд
            }

            currentRecorder.onKeyPressed(keyCode);
        }
    }

    public static class KeyBindRecorder {
        private final String keyBindId;
        private final KeyBindRecorderCallback callback;
        private String key1 = null; // Первая кнопка
        private String key2 = null; // Вторая кнопка

        public KeyBindRecorder(String keyBindId, KeyBindRecorderCallback callback) {
            this.keyBindId = keyBindId;
            this.callback = callback;
        }

        public void onKeyPressed(int keyCode) {
            String keyName = KeyBindings.getKeyName(keyCode);
            if (keyName == null) return;

            // SPACE - завершить запись
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                if (key1 != null) {
                    // SPACE после первой кнопки - завершить с одной кнопкой
                    saveSingleKey(key1);
                } else {
                    // SPACE без кнопок - отмена
                    callback.onRecordingCancelled();
                }
                return;
            }

            if (key1 == null) {
                // Первая кнопка
                key1 = keyName;
                callback.onKey1Recorded(keyName);
                callback.onWaitingForKey2();
            } else if (key2 == null && !keyName.equals(key1)) {
                // Вторая кнопка (не та же самая)
                key2 = keyName;
                saveKeyCombination(key1, key2);
            }
            // Игнорируем повторные нажатия той же кнопки
        }

        private void saveSingleKey(String key) {
            // Одна кнопка: key становится основной, модификатора нет
            Configs.KeyBindConfig newConfig = new Configs.KeyBindConfig(key, null);
            saveConfig(newConfig);
        }

        private void saveKeyCombination(String key1, String key2) {
            // Две кнопки: key1 становится модификатором, key2 становится основной
            Configs.KeyBindConfig newConfig = new Configs.KeyBindConfig(key2, key1);
            saveConfig(newConfig);
        }

        private void saveConfig(Configs.KeyBindConfig newConfig) {
            // Проверяем конфликты
            if (hasKeyBindConflict(keyBindId, newConfig)) {
                callback.onRecordingFailed("Эта комбинация уже используется для другой функции");
                return;
            }

            // Сохраняем новую комбинацию
            updateKeyBindConfig(keyBindId, newConfig);
            Configs.saveConfig();

            callback.onRecordingCompleted(newConfig);
        }
    }

    public interface KeyBindRecorderCallback {
        void onKey1Recorded(String keyName);
        void onWaitingForKey2();
        void onRecordingCompleted(Configs.KeyBindConfig newConfig);
        void onRecordingFailed(String error);
        void onRecordingCancelled();
    }

    private static boolean hasKeyBindConflict(String currentId, Configs.KeyBindConfig newConfig) {
        Map<String, Configs.KeyBindConfig> allConfigs = getAllKeyBindConfigs();

        for (Map.Entry<String, Configs.KeyBindConfig> entry : allConfigs.entrySet()) {
            if (!entry.getKey().equals(currentId) && newConfig.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Configs.KeyBindConfig> getAllKeyBindConfigs() {
        Map<String, Configs.KeyBindConfig> configs = new HashMap<>();
        configs.put("group_next", Configs.getConfig().group_next);
        configs.put("group_previous", Configs.getConfig().group_previous);
        configs.put("end_navigation", Configs.getConfig().end_navigation);
        configs.put("quick_shop", Configs.getConfig().quick_shop);
        return configs;
    }

    private static void updateKeyBindConfig(String id, Configs.KeyBindConfig newConfig) {
        switch (id) {
            case "group_next":
                Configs.getConfig().group_next = newConfig;
                break;
            case "group_previous":
                Configs.getConfig().group_previous = newConfig;
                break;
            case "end_navigation":
                Configs.getConfig().end_navigation = newConfig;
                break;
            case "quick_shop":
                Configs.getConfig().quick_shop = newConfig;
                break;
        }
    }

    private static Configs.KeyBindConfig getKeyBindConfig(String id) {
        switch (id) {
            case "group_next": return Configs.getConfig().group_next;
            case "group_previous": return Configs.getConfig().group_previous;
            case "end_navigation": return Configs.getConfig().end_navigation;
            case "quick_shop": return Configs.getConfig().quick_shop;
            default: return null;
        }
    }
}