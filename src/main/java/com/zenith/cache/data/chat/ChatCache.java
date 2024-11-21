package com.zenith.cache.data.chat;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE_LOG;
import static com.zenith.Shared.CONFIG;

@Data
@Accessors(chain = true)
public class ChatCache implements CachedData {
    protected CommandNode[] commandNodes = new CommandNode[0];
    protected int firstCommandNodeIndex;
    protected volatile long lastChatTimestamp = System.currentTimeMillis();
    protected boolean enforcesSecureChat = false;
    protected @Nullable ChatSession chatSession = new ChatSession(UUID.randomUUID());
    protected @Nullable StepPlayerCertificates.PlayerCertificates playerCertificates;
    protected ChatTypeRegistry chatTypeRegistry = new ChatTypeRegistry();

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        consumer.accept(new ClientboundCommandsPacket(this.commandNodes, this.firstCommandNodeIndex));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.PROTOCOL_SWITCH || type == CacheResetType.FULL) {
            this.commandNodes = new CommandNode[0];
            this.firstCommandNodeIndex = 0;
        }
        if (type == CacheResetType.FULL) {
            this.enforcesSecureChat = false;
            this.chatTypeRegistry.reset();
            this.lastChatTimestamp = System.currentTimeMillis();
        }
    }

    @Override
    public String getSendingMessage()  {
        return String.format("Sending %s server commands", this.commandNodes.length);
    }

    public boolean canUseChatSigning() {
        return this.enforcesSecureChat && this.playerCertificates != null && CONFIG.client.chatSigning.enabled;
    }

    public ChatSession startNewChatSession() {
        this.chatSession = new ChatSession(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (this.playerCertificates == null) {
            CACHE_LOG.error("Initializing chat session without player certificates");
        }
        this.chatSession.setPlayerCertificates(this.playerCertificates);
        return this.chatSession;
    }

    public void initializeChatTypeRegistry(List<RegistryEntry> registryEntries) {
        chatTypeRegistry.initialize(registryEntries);
    }
}
