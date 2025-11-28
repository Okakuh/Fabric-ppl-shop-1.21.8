package net.okakuh.pepelandshop.util;

import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class KeyBindings {
    // Полный список всех клавиш GLFW с их кодами и названиями
    public static final String[][] ALL_KEYS = {
            // Функциональные клавиши
            {"f1", "F1", String.valueOf(InputUtil.GLFW_KEY_F1)},
            {"f2", "F2", String.valueOf(InputUtil.GLFW_KEY_F2)},
            {"f3", "F3", String.valueOf(InputUtil.GLFW_KEY_F3)},
            {"f4", "F4", String.valueOf(InputUtil.GLFW_KEY_F4)},
            {"f5", "F5", String.valueOf(InputUtil.GLFW_KEY_F5)},
            {"f6", "F6", String.valueOf(InputUtil.GLFW_KEY_F6)},
            {"f7", "F7", String.valueOf(InputUtil.GLFW_KEY_F7)},
            {"f8", "F8", String.valueOf(InputUtil.GLFW_KEY_F8)},
            {"f9", "F9", String.valueOf(InputUtil.GLFW_KEY_F9)},
            {"f10", "F10", String.valueOf(InputUtil.GLFW_KEY_F10)},
            {"f11", "F11", String.valueOf(InputUtil.GLFW_KEY_F11)},
            {"f12", "F12", String.valueOf(InputUtil.GLFW_KEY_F12)},
            {"f13", "F13", String.valueOf(InputUtil.GLFW_KEY_F13)},
            {"f14", "F14", String.valueOf(InputUtil.GLFW_KEY_F14)},
            {"f15", "F15", String.valueOf(InputUtil.GLFW_KEY_F15)},
            {"f16", "F16", String.valueOf(InputUtil.GLFW_KEY_F16)},
            {"f17", "F17", String.valueOf(InputUtil.GLFW_KEY_F17)},
            {"f18", "F18", String.valueOf(InputUtil.GLFW_KEY_F18)},
            {"f19", "F19", String.valueOf(InputUtil.GLFW_KEY_F19)},
            {"f20", "F20", String.valueOf(InputUtil.GLFW_KEY_F20)},
            {"f21", "F21", String.valueOf(InputUtil.GLFW_KEY_F21)},
            {"f22", "F22", String.valueOf(InputUtil.GLFW_KEY_F22)},
            {"f23", "F23", String.valueOf(InputUtil.GLFW_KEY_F23)},
            {"f24", "F24", String.valueOf(InputUtil.GLFW_KEY_F24)},
            {"f25", "F25", String.valueOf(InputUtil.GLFW_KEY_F25)},

            // Цифровые клавиши
            {"0", "0", String.valueOf(InputUtil.GLFW_KEY_0)},
            {"1", "1", String.valueOf(InputUtil.GLFW_KEY_1)},
            {"2", "2", String.valueOf(InputUtil.GLFW_KEY_2)},
            {"3", "3", String.valueOf(InputUtil.GLFW_KEY_3)},
            {"4", "4", String.valueOf(InputUtil.GLFW_KEY_4)},
            {"5", "5", String.valueOf(InputUtil.GLFW_KEY_5)},
            {"6", "6", String.valueOf(InputUtil.GLFW_KEY_6)},
            {"7", "7", String.valueOf(InputUtil.GLFW_KEY_7)},
            {"8", "8", String.valueOf(InputUtil.GLFW_KEY_8)},
            {"9", "9", String.valueOf(InputUtil.GLFW_KEY_9)},

            // Буквенные клавиши
            {"a", "A", String.valueOf(InputUtil.GLFW_KEY_A)},
            {"b", "B", String.valueOf(InputUtil.GLFW_KEY_B)},
            {"c", "C", String.valueOf(InputUtil.GLFW_KEY_C)},
            {"d", "D", String.valueOf(InputUtil.GLFW_KEY_D)},
            {"e", "E", String.valueOf(InputUtil.GLFW_KEY_E)},
            {"f", "F", String.valueOf(InputUtil.GLFW_KEY_F)},
            {"g", "G", String.valueOf(InputUtil.GLFW_KEY_G)},
            {"h", "H", String.valueOf(InputUtil.GLFW_KEY_H)},
            {"i", "I", String.valueOf(InputUtil.GLFW_KEY_I)},
            {"j", "J", String.valueOf(InputUtil.GLFW_KEY_J)},
            {"k", "K", String.valueOf(InputUtil.GLFW_KEY_K)},
            {"l", "L", String.valueOf(InputUtil.GLFW_KEY_L)},
            {"m", "M", String.valueOf(InputUtil.GLFW_KEY_M)},
            {"n", "N", String.valueOf(InputUtil.GLFW_KEY_N)},
            {"o", "O", String.valueOf(InputUtil.GLFW_KEY_O)},
            {"p", "P", String.valueOf(InputUtil.GLFW_KEY_P)},
            {"q", "Q", String.valueOf(InputUtil.GLFW_KEY_Q)},
            {"r", "R", String.valueOf(InputUtil.GLFW_KEY_R)},
            {"s", "S", String.valueOf(InputUtil.GLFW_KEY_S)},
            {"t", "T", String.valueOf(InputUtil.GLFW_KEY_T)},
            {"u", "U", String.valueOf(InputUtil.GLFW_KEY_U)},
            {"v", "V", String.valueOf(InputUtil.GLFW_KEY_V)},
            {"w", "W", String.valueOf(InputUtil.GLFW_KEY_W)},
            {"x", "X", String.valueOf(InputUtil.GLFW_KEY_X)},
            {"y", "Y", String.valueOf(InputUtil.GLFW_KEY_Y)},
            {"z", "Z", String.valueOf(InputUtil.GLFW_KEY_Z)},

            // Специальные клавиши
            {"space", "Space", String.valueOf(InputUtil.GLFW_KEY_SPACE)},
            {"apostrophe", "'", String.valueOf(InputUtil.GLFW_KEY_APOSTROPHE)},
            {"comma", ",", String.valueOf(InputUtil.GLFW_KEY_COMMA)},
            {"minus", "-", String.valueOf(InputUtil.GLFW_KEY_MINUS)},
            {"period", ".", String.valueOf(InputUtil.GLFW_KEY_PERIOD)},
            {"slash", "/", String.valueOf(InputUtil.GLFW_KEY_SLASH)},
            {"semicolon", ";", String.valueOf(InputUtil.GLFW_KEY_SEMICOLON)},
            {"equal", "=", String.valueOf(InputUtil.GLFW_KEY_EQUAL)},
            {"left_bracket", "[", String.valueOf(InputUtil.GLFW_KEY_LEFT_BRACKET)},
            {"backslash", "\\", String.valueOf(InputUtil.GLFW_KEY_BACKSLASH)},
            {"right_bracket", "]", String.valueOf(InputUtil.GLFW_KEY_RIGHT_BRACKET)},
            {"grave_accent", "`", String.valueOf(InputUtil.GLFW_KEY_GRAVE_ACCENT)},

            // Клавиши управления
            {"escape", "Escape", String.valueOf(InputUtil.GLFW_KEY_ESCAPE)},
            {"enter", "Enter", String.valueOf(InputUtil.GLFW_KEY_ENTER)},
            {"tab", "Tab", String.valueOf(InputUtil.GLFW_KEY_TAB)},
            {"backspace", "Backspace", String.valueOf(InputUtil.GLFW_KEY_BACKSPACE)},
            {"insert", "Insert", String.valueOf(InputUtil.GLFW_KEY_INSERT)},
            {"delete", "Delete", String.valueOf(InputUtil.GLFW_KEY_DELETE)},
            {"right", "Right Arrow", String.valueOf(InputUtil.GLFW_KEY_RIGHT)},
            {"left", "Left Arrow", String.valueOf(InputUtil.GLFW_KEY_LEFT)},
            {"down", "Down Arrow", String.valueOf(InputUtil.GLFW_KEY_DOWN)},
            {"up", "Up Arrow", String.valueOf(InputUtil.GLFW_KEY_UP)},
            {"page_up", "Page Up", String.valueOf(InputUtil.GLFW_KEY_PAGE_UP)},
            {"page_down", "Page Down", String.valueOf(InputUtil.GLFW_KEY_PAGE_DOWN)},
            {"home", "Home", String.valueOf(InputUtil.GLFW_KEY_HOME)},
            {"end", "End", String.valueOf(InputUtil.GLFW_KEY_END)},
            {"caps_lock", "Caps Lock", String.valueOf(InputUtil.GLFW_KEY_CAPS_LOCK)},
            {"scroll_lock", "Scroll Lock", String.valueOf(InputUtil.GLFW_KEY_SCROLL_LOCK)},
            {"num_lock", "Num Lock", String.valueOf(InputUtil.GLFW_KEY_NUM_LOCK)},
            {"print_screen", "Print Screen", String.valueOf(InputUtil.GLFW_KEY_PRINT_SCREEN)},
            {"pause", "Pause", String.valueOf(InputUtil.GLFW_KEY_PAUSE)},

            // Модификаторы
            {"left_shift", "Left Shift", String.valueOf(InputUtil.GLFW_KEY_LEFT_SHIFT)},
            {"left_control", "Left Control", String.valueOf(InputUtil.GLFW_KEY_LEFT_CONTROL)},
            {"left_alt", "Left Alt", String.valueOf(InputUtil.GLFW_KEY_LEFT_ALT)},
            {"left_super", "Left Super", String.valueOf(InputUtil.GLFW_KEY_LEFT_SUPER)},
            {"right_shift", "Right Shift", String.valueOf(InputUtil.GLFW_KEY_RIGHT_SHIFT)},
            {"right_control", "Right Control", String.valueOf(InputUtil.GLFW_KEY_RIGHT_CONTROL)},
            {"right_alt", "Right Alt", String.valueOf(InputUtil.GLFW_KEY_RIGHT_ALT)},
            {"right_super", "Right Super", String.valueOf(InputUtil.GLFW_KEY_RIGHT_SUPER)},

            // Цифровая клавиатура
            {"kp_0", "Num 0", String.valueOf(InputUtil.GLFW_KEY_KP_0)},
            {"kp_1", "Num 1", String.valueOf(InputUtil.GLFW_KEY_KP_1)},
            {"kp_2", "Num 2", String.valueOf(InputUtil.GLFW_KEY_KP_2)},
            {"kp_3", "Num 3", String.valueOf(InputUtil.GLFW_KEY_KP_3)},
            {"kp_4", "Num 4", String.valueOf(InputUtil.GLFW_KEY_KP_4)},
            {"kp_5", "Num 5", String.valueOf(InputUtil.GLFW_KEY_KP_5)},
            {"kp_6", "Num 6", String.valueOf(InputUtil.GLFW_KEY_KP_6)},
            {"kp_7", "Num 7", String.valueOf(InputUtil.GLFW_KEY_KP_7)},
            {"kp_8", "Num 8", String.valueOf(InputUtil.GLFW_KEY_KP_8)},
            {"kp_9", "Num 9", String.valueOf(InputUtil.GLFW_KEY_KP_9)},
            {"kp_decimal", "Num .", String.valueOf(InputUtil.GLFW_KEY_KP_DECIMAL)},
            {"kp_multiply", "Num *", String.valueOf(InputUtil.GLFW_KEY_KP_MULTIPLY)},
            {"kp_add", "Num +", String.valueOf(InputUtil.GLFW_KEY_KP_ADD)},
            {"kp_enter", "Num Enter", String.valueOf(InputUtil.GLFW_KEY_KP_ENTER)},
            {"kp_equal", "Num =", String.valueOf(InputUtil.GLFW_KEY_KP_EQUAL)}
    };

    // Метод для получения кода клавиши по имени
    public static int getKeyCode(String keyName) {
        for (String[] key : ALL_KEYS) {
            if (key[0].equalsIgnoreCase(keyName)) {
                return Integer.parseInt(key[2]);
            }
        }
        return -1; // Не найдено
    }

    // Метод для получения имени клавиши по коду
    public static String getKeyName(int keyCode) {
        for (String[] key : ALL_KEYS) {
            if (Integer.parseInt(key[2]) == keyCode) {
                return key[0];
            }
        }
        return null; // Не найдено
    }

    // Метод для получения отображаемого имени клавиши
    public static String getDisplayName(String keyName) {
        for (String[] key : ALL_KEYS) {
            if (key[0].equalsIgnoreCase(keyName)) {
                return key[1];
            }
        }
        return keyName; // Возвращаем исходное имя, если не найдено
    }
}