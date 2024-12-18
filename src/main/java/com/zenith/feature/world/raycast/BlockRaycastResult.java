package com.zenith.feature.world.raycast;

import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

public record BlockRaycastResult(boolean hit, int x, int y, int z, Direction direction, Block block) {
    public static BlockRaycastResult miss() {
        return new BlockRaycastResult(false, 0, 0, 0, Direction.UP, BlockRegistry.AIR);
    }
}
