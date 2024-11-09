package com.zenith.via;

import com.zenith.feature.queue.mcping.MCPing;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtocolVersionDetector {
    public int getProtocolVersion(final String hostname, final int port) {
        try {
            return MCPing.INSTANCE.getProtocolVersion(hostname, port, 3000, true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
