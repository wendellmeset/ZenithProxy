package com.zenith.mc.block;

import com.zenith.mc.RegistryData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public record Block(
    int id,
    String name,
    boolean isBlock,
    int minStateId,
    int maxStateId,
    int mapColorId,
    BlockOffsetType offsetType,
    float maxHorizontalOffset,
    float maxVerticalOffset,
    float destroySpeed,
    boolean requiresCorrectToolForDrops,
    EnumSet<BlockTags> blockTags,
    @Nullable BlockEntityType blockEntityType
) implements RegistryData {
    public Block(
        int id,
        String name,
        boolean isBlock,
        int minStateId,
        int maxStateId,
        int mapColorId,
        BlockOffsetType offsetType,
        float maxHorizontalOffset,
        float maxVerticalOffset,
        float destroySpeed,
        boolean requiresCorrectToolForDrops
    ) {
        this(id, name, isBlock, minStateId, maxStateId, mapColorId, offsetType, maxHorizontalOffset, maxVerticalOffset, destroySpeed, requiresCorrectToolForDrops, EnumSet.noneOf(BlockTags.class), null);
    }

    public Block(
        int id,
        String name,
        boolean isBlock,
        int minStateId,
        int maxStateId,
        int mapColorId,
        BlockOffsetType offsetType,
        float maxHorizontalOffset,
        float maxVerticalOffset,
        float destroySpeed,
        boolean requiresCorrectToolForDrops,
        BlockEntityType blockEntityType
    ) {
        this(id, name, isBlock, minStateId, maxStateId, mapColorId, offsetType, maxHorizontalOffset, maxVerticalOffset, destroySpeed, requiresCorrectToolForDrops, EnumSet.noneOf(BlockTags.class), blockEntityType);
    }

    public Block(
        int id,
        String name,
        boolean isBlock,
        int minStateId,
        int maxStateId,
        int mapColorId,
        BlockOffsetType offsetType,
        float maxHorizontalOffset,
        float maxVerticalOffset,
        float destroySpeed,
        boolean requiresCorrectToolForDrops,
        EnumSet<BlockTags> blockTags
    ) {
        this(id, name, isBlock, minStateId, maxStateId, mapColorId, offsetType, maxHorizontalOffset, maxVerticalOffset, destroySpeed, requiresCorrectToolForDrops, blockTags, null);
    }
}
