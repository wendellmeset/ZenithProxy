package com.zenith.cache.data.chat;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ChatType(int id, String translationKey, List<String> parameters) {
    public Component render(@Nullable Component sender, @Nullable Component content, @Nullable Component target) {
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
    }
}
