package com.zenith.network.client.handler.incoming;

import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;

import static com.zenith.Shared.*;

public class SetHealthHandler implements ClientEventLoopPacketHandler<ClientboundSetHealthPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundSetHealthPacket packet, @NonNull ClientSession session) {
        var player = CACHE.getPlayerCache().getThePlayer();
        if (packet.getHealth() != player.getHealth()) {
            EVENT_BUS.postAsync(
                new PlayerHealthChangedEvent(packet.getHealth(), player.getHealth()));
        }

        if (packet.getFood() != player.getFood())
            CACHE_LOG.debug("Player food: {}", packet.getFood());
        if (packet.getSaturation() != player.getSaturation())
            CACHE_LOG.debug("Player saturation: {}", packet.getSaturation());
        if (packet.getHealth() != player.getHealth())
            CACHE_LOG.debug("Player health: {}", packet.getHealth());

        player
            .setFood(packet.getFood())
            .setSaturation(packet.getSaturation())
            .setHealth(packet.getHealth());
        return true;
    }
}
