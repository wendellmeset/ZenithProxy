package com.zenith.network.client.handler.incoming;

import com.zenith.Shared;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.Config;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;

import javax.crypto.SecretKey;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EXECUTOR;

public class CHelloHandler implements PacketHandler<ClientboundHelloPacket, ClientSession> {
    @Override
    public ClientboundHelloPacket apply(final ClientboundHelloPacket packet, final ClientSession session) {
        final GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        final String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

        if (CONFIG.authentication.accountType == Config.Authentication.AccountType.OFFLINE) {
            if (packet.isShouldAuthenticate()) {
                session.disconnect(Shared.AUTH_REQUIRED);
                return null;
            }
        } else {
            if (profile == null || accessToken == null) {
                session.disconnect("No Profile or Access Token provided.");
                return null;
            }
        }

        final SecretKey key = SessionServerApi.INSTANCE.generateClientKey();
        if (key == null) {
            session.disconnect("Failed to generate secret key.");
            return null;
        }
        if (packet.isShouldAuthenticate()) {
            EXECUTOR.execute(() -> {
                try {
                    final String sharedSecret = SessionServerApi.INSTANCE.getSharedSecret(packet.getServerId(), packet.getPublicKey(), key);
                    SessionServerApi.INSTANCE.joinServer(profile.getId(), accessToken, sharedSecret);
                } catch (Exception e) {
                    session.disconnect("Login failed: Authentication service unavailable.", e);
                    return;
                }
                session.send(new ServerboundKeyPacket(packet.getPublicKey(), key, packet.getChallenge()),
                             (f) -> session.enableEncryption(key));
            });
        } else {
            session.send(new ServerboundKeyPacket(packet.getPublicKey(), key, packet.getChallenge()));
            session.enableEncryption(key);
        }
        return null;
    }
}
