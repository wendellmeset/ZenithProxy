package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import com.zenith.feature.extrachat.ECChatCommandIncomingHandler;
import com.zenith.feature.extrachat.ECPlayerChatOutgoingHandler;
import com.zenith.feature.extrachat.ECSignedChatCommandIncomingHandler;
import com.zenith.feature.extrachat.ECSystemChatOutgoingHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.PacketHandlerStateCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;
import static java.util.Objects.nonNull;

public class ExtraChat extends Module {
    private final PacketHandlerCodec codec = PacketHandlerCodec.builder()
        .setId("extra-chat")
        .setPriority(-1)
        .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerSession>builder()
            .allowUnhandledInbound(true)
            .registerOutbound(ClientboundSystemChatPacket.class, new ECSystemChatOutgoingHandler())
            .registerOutbound(ClientboundPlayerChatPacket.class, new ECPlayerChatOutgoingHandler())
            .registerInbound(ServerboundChatCommandPacket.class, new ECChatCommandIncomingHandler())
            .registerInbound(ServerboundChatCommandSignedPacket.class, new ECSignedChatCommandIncomingHandler())
            .build())
        .build();

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.chat.enabled;
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnected),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnected)
        );
    }

    @Override
    public void onEnable() {
        ZenithHandlerCodec.SERVER_REGISTRY.register(codec);
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.SERVER_REGISTRY.unregister(codec);
    }

    private void handleServerPlayerDisconnected(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<aqua>" + event.playerEntry().getName() + "<yellow> disconnected"), false));
    }

    private void handleServerPlayerConnected(ServerPlayerConnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<aqua" + event.playerEntry().getName() + "<yellow> connected"), false));
    }

}
