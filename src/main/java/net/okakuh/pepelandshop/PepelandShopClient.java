package net.okakuh.pepelandshop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.okakuh.pepelandshop.commands.ShopCommands;
import net.okakuh.pepelandshop.commands.ConfigCommands;
import net.okakuh.pepelandshop.managers.KeyBindManager;
import net.okakuh.pepelandshop.managers.NavigationManager;
import org.lwjgl.glfw.GLFW;

import static net.okakuh.pepelandshop.Configs.*;

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
                consumeAllKeysForRecording(client);
            } else {
                // Обычная логика навигации
                NavigationManager.handleKeyNavigation(client);
                NavigationManager.handleQuickShop(client);
            }
        });
    }

    private static void consumeAllKeysForRecording(net.minecraft.client.MinecraftClient client) {
        // Во время записи перехватываем все клавиши
        for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS) {
                KeyBindManager.handleKeyInput(keyCode, GLFW.GLFW_PRESS);
                break; // Обрабатываем только одну клавишу за тик
            }
        }
    }
}