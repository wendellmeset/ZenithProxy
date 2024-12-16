package com.zenith.mc.item;

import lombok.Getter;

@Getter
public enum ToolTier {
    WOOD(2.0F),
    STONE(4.0F),
    IRON(6.0F),
    DIAMOND(8.0F),
    GOLD(12.0F),
    NETHERITE(9.0f);

    private final float speed;

    ToolTier(float speed) {
        this.speed = speed;
    }
}
