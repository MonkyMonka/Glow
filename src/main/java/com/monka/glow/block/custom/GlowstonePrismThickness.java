package com.monka.glow.block.custom;

import net.minecraft.util.StringRepresentable;

public enum GlowstonePrismThickness implements StringRepresentable {
    TIP("tip"),
    MIDDLE("middle"),
    BASE("base");

    private final String name;

    private GlowstonePrismThickness(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public String getSerializedName() {
        return this.name;
    }
}