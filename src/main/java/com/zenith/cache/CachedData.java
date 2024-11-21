package com.zenith.cache;

import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;

import java.util.function.Consumer;


public interface CachedData {
    void getPackets(@NonNull Consumer<Packet> consumer, @NonNull final TcpSession session);

    void reset(CacheResetType type);

    default String getSendingMessage()  {
        return null;
    }
}
