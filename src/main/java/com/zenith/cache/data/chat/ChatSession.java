package com.zenith.cache.data.chat;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import lombok.Data;
import lombok.experimental.Accessors;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import org.geysermc.mcprotocollib.protocol.data.game.ArgumentSignature;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.zenith.Shared.CACHE;

@Data
@Accessors(chain = true)
public class ChatSession {
    private final UUID sessionId;
    protected StepPlayerCertificates.PlayerCertificates playerCertificates;
    protected int chainIndex = 0;

    public void sign(ServerboundChatPacket packet) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(playerCertificates.getPrivateKey());
            signature.update(Ints.toByteArray(1));
            // message link
            signature.update(uuidToByteArray(CACHE.getProfileCache().getProfile().getId()));
            signature.update(uuidToByteArray(sessionId));
            signature.update(Ints.toByteArray(chainIndex++));

            // message body
            signature.update(Longs.toByteArray(packet.getSalt()));
            signature.update(Longs.toByteArray(packet.getTimeStamp() / 1000));
            var bs = packet.getMessage().getBytes(StandardCharsets.UTF_8);
            signature.update(Ints.toByteArray(bs.length));
            signature.update(bs);
            // last seen messages list (always empty)
            signature.update(Ints.toByteArray(0));
            var sign = signature.sign();
            packet.setSignature(sign);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    // todo: only works for whisper command
    public void sign(final ServerboundChatCommandSignedPacket packet) {
        try {
            List<String> split = Arrays.asList(packet.getCommand().split(" "));
            if (split.size() < 3) {
                throw new IllegalArgumentException("Invalid command");
            }
            String msg = String.join(" ", split.subList(2, split.size()));

            List<ArgumentSignature> signatures = new ArrayList<>(1);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(playerCertificates.getPrivateKey());
            signature.update(Ints.toByteArray(1));
            // message link
            signature.update(uuidToByteArray(CACHE.getProfileCache().getProfile().getId()));
            signature.update(uuidToByteArray(sessionId));
            signature.update(Ints.toByteArray(chainIndex++));

            // message body
            signature.update(Longs.toByteArray(packet.getSalt()));
            signature.update(Longs.toByteArray(packet.getTimeStamp() / 1000));
            var bs = msg.getBytes(StandardCharsets.UTF_8);
            signature.update(Ints.toByteArray(bs.length));
            signature.update(bs);
            // last seen messages list (always empty)
            signature.update(Ints.toByteArray(0));
            var sign = signature.sign();
            signatures.add(new ArgumentSignature("message", sign));
            packet.setSignatures(signatures);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign command", e);
        }
    }

    public static byte[] uuidToByteArray(UUID uuid) {
        byte[] bs = new byte[16];
        ByteBuffer.wrap(bs).order(ByteOrder.BIG_ENDIAN).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return bs;
    }

    public record LastSeenTrackedEntry(byte[] signature) {}
}
