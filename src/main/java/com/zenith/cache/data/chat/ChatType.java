package com.zenith.cache.data.chat;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.CLIENT_LOG;

public record ChatType(int id, String translationKey, List<String> parameters) {
    public Component render(@Nullable Component sender, @Nullable Component content, @Nullable Component target) {
        try {
            List<Component> args = new ArrayList<>(parameters.size());
            for (var parameter : parameters) {
                switch (parameter) {
                    case "sender" -> args.add(sender);
                    case "content" -> args.add(content);
                    case "target" -> args.add(target);
                }
            }
            return Component
                .translatable(translationKey)
                .arguments(args);
        } catch (final Exception e) {
            CLIENT_LOG.warn("Failed to render chat type: {}", translationKey, e);
            return Component.translatable(translationKey);
        }
    }
}
