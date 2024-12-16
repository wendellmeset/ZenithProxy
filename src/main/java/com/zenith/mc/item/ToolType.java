package com.zenith.mc.item;

import com.zenith.mc.block.BlockTags;
import lombok.Getter;

@Getter
public enum ToolType {
    AXE(BlockTags.MINEABLE_WITH_AXE),
    HOE(BlockTags.MINEABLE_WITH_HOE),
    PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
    SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
    SWORD(BlockTags.SWORD_EFFICIENT);

    private final BlockTags blockTag;

    ToolType(BlockTags blockTag) {
        this.blockTag = blockTag;
    }
}
