package dev.aurora.gui;

import org.bukkit.Material;

/**
 * Fluent builder for customizing command GUIs.
 */
public class GUIBuilder {
    private String title;
    private int size;
    private Material executeButton;

    private GUIBuilder() {
        this.title = "§6Command Menu";
        this.size = 27;
        this.executeButton = Material.EMERALD;
    }

    public static GUIBuilder create() {
        return new GUIBuilder();
    }

    public GUIBuilder title(String title) {
        this.title = title;
        return this;
    }

    public GUIBuilder size(int rows) {
        this.size = rows * 9;
        return this;
    }

    public GUIBuilder executeButton(Material material) {
        this.executeButton = material;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public Material getExecuteButton() {
        return executeButton;
    }
}
