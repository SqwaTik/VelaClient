package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.module.ModuleCategory;

public enum VelaSection {
    VISUALS("Visuals", "Настройка изображения и атмосферы", ModuleCategory.VISUALS),
    HUD("HUD", "Редактор экранных элементов", ModuleCategory.HUD),
    UTILITIES("Utilities", "Удобные клиентские инструменты", ModuleCategory.UTILITIES),
    MARKERS("Markers", "Локальные точки и ориентиры", ModuleCategory.MARKERS),
    FRIENDS("Friends", "Ваш локальный список друзей", null),
    CONFIGS("Configs", "Управление профилями", null);

    private final String title;
    private final String description;
    private final ModuleCategory category;

    VelaSection(String title, String description, ModuleCategory category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public String title() { return title; }
    public String description() { return description; }
    public ModuleCategory category() { return category; }
}
