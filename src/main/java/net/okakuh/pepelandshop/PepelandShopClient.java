package net.okakuh.pepelandshop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.okakuh.pepelandshop.command.ShopCommands;
import net.okakuh.pepelandshop.config.ConfigCommands;
import net.okakuh.pepelandshop.config.KeyBindManager;
import net.okakuh.pepelandshop.navigation.NavigationManager;
import org.lwjgl.glfw.GLFW;

import static net.okakuh.pepelandshop.config.ConfigManager.*;

public class PepelandShopClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        loadConfig();

        ConfigCommands.registerCommands();
        ShopCommands.registerCommands();

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            net.okakuh.pepelandshop.render.BlockHighlighter.render(context);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;

            // Если идет запись клавиш
            if (KeyBindManager.isRecording()) {
                // Просто обрабатываем запись
                for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
                    if (GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS) {
                        KeyBindManager.handleKeyInput(keyCode, GLFW.GLFW_PRESS);
                        break;
                    }
                }
            } else {
                // Обычная логика - Mixin сам перехватит клавиши
                NavigationManager.handleKeyNavigation(client);
                NavigationManager.handleQuickShop(client);
            }
        });
    }

    private static boolean shouldConsumeInput() {
        // Проверяем, нажата ли любая из наших клавиш
        return KeyBindManager.isPressed("group_next") ||
                KeyBindManager.isPressed("group_previous") ||
                KeyBindManager.isPressed("end_navigation") ||
                KeyBindManager.isPressed("quick_shop");
    }

    private static void consumeKeyEvents(net.minecraft.client.MinecraftClient client) {
        // Перехватываем события клавиш, чтобы они не доходили до Minecraft
        // Это предотвратит выполнение стандартных действий Minecraft
        // Например, если R назначена на быстрый магазин, она не будет открывать инвентарь
    }
}