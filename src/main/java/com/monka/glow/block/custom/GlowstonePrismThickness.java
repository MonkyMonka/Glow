package com.monka.glow.block.custom;

import net.minecraft.util.StringRepresentable;

public enum GlowstonePrismThickness implements StringRepresentable {
    TIP("tip", 0),
    MIDDLE("middle", 1),
    BASE("base", 2);

    private final String name;
    private final int size;

    private GlowstonePrismThickness(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String toString() {
        return this.name;
    }

    public String getSerializedName() {
        return this.name;
    }

    public int getSize() {
        return this.size;
    }
}