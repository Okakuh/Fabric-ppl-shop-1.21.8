package net.okakuh.pepelandshop;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class PepelandShopSettingsScreen extends Screen {
    private int currentTab = 0; // 0=Дефолт, 1=Поиск, 2=Визуал, 3=Кийбаинды

    public PepelandShopSettingsScreen() {
        super(Text.literal("Pepeland Shop Settings"));
    }

    @Override
    protected void init() {
        super.init();

        // Кнопки вкладок
        int tabWidth = this.width / 4;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Дефолт"), button -> {
            currentTab = 0;
            clearAndInit();
        }).dimensions(0, 0, tabWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Поиск"), button -> {
            currentTab = 1;
            clearAndInit();
        }).dimensions(tabWidth, 0, tabWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Визуал"), button -> {
            currentTab = 2;
            clearAndInit();
        }).dimensions(tabWidth * 2, 0, tabWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Кийбаинды"), button -> {
            currentTab = 3;
            clearAndInit();
        }).dimensions(tabWidth * 3, 0, tabWidth, 20).build());

        // Содержимое вкладок
        switch (currentTab) {
            case 0 -> initDefaultTab();
            case 1 -> initSearchTab();
            case 2 -> initVisualTab();
            case 3 -> initKeybindsTab();
        }

        // Кнопка назад
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void initDefaultTab() {
        int centerX = this.width / 2;
        int y = 40;

        // Радиус по умолчанию
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Радиус: " + ConfigManager.getConfig().default_radius), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Стак по умолчанию
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Стак: " + ConfigManager.getConfig().default_stack), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y + 30, 200, 20).build());
    }

    private void initSearchTab() {
        int centerX = this.width / 2;
        int y = 40;

        // Y координаты
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Y: " + ConfigManager.getConfig().y_coords), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Паттерн цены
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Паттерн цены"), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y + 30, 200, 20).build());

        // Паттерн количества
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Паттерн количества"), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y + 60, 200, 20).build());
    }

    private void initVisualTab() {
        int centerX = this.width / 2;
        int y = 40;

        // Цвет подсветки
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Цвет подсветки"), button -> {
            // Будущий функционал изменения
        }).dimensions(centerX - 100, y, 200, 20).build());
    }

    private void initKeybindsTab() {
        int centerX = this.width / 2;
        int y = 40;

        // Быстрый магазин
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Быстрый магазин"), button -> {
            // Будущий функционал настройки
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Навигация
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Навигация"), button -> {
            // Будущий функционал настройки
        }).dimensions(centerX - 100, y + 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Вместо renderBackground используем простой фон
        context.fill(0, 0, this.width, this.height, 0xFF1E1E1E);

        // Рамка вокруг контента
        context.fill(0, 20, this.width, this.height, 0xFF2D2D2D);

        // Заголовок текущей вкладки
        String[] tabNames = {"Основные настройки", "Настройки поиска", "Визуальные настройки", "Настройки клавиш"};
        context.drawCenteredTextWithShadow(this.textRenderer, tabNames[currentTab], this.width / 2, 25, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}