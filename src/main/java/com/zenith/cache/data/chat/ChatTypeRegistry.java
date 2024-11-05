package com.zenith.cache.data.chat;

import com.viaversion.nbt.io.MNBTIO;
import com.viaversion.nbt.mini.MNBT;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.List;

import static com.zenith.Shared.CACHE_LOG;

@Data
public class ChatTypeRegistry {
    protected final Int2ObjectMap<ChatType> chatTypes = new Int2ObjectOpenHashMap<>();

    public void initialize(List<RegistryEntry> registryEntries) {
        for (int i = 0; i < registryEntries.size(); i++) {
            final var entry = registryEntries.get(i);
            MNBT mnbt = entry.getData();
            try {
                var topTag = (CompoundTag) MNBTIO.read(mnbt);
                var chatTag = topTag.getCompoundTag("chat");
                var translationKey = chatTag.getString("translation_key");
                var parametersList = chatTag.getListTag("parameters", StringTag.class);
                var parameters = parametersList.stream().map(StringTag::getValue).toList();
                chatTypes.put(i, new ChatType(i, translationKey, parameters));
            } catch (final Exception e) {
                CACHE_LOG.error("Failed to parse chat type registry entry {} : {}", i, entry.getId(), e);
            }
        }
    }

    public ChatType getChatType(int id) {
        return chatTypes.get(id);
    }

    public void reset() {
        chatTypes.clear();
    }
}
