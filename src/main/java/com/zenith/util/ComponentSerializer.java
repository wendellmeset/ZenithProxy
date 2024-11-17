package com.zenith.util;

import com.zenith.discord.Embed;
import com.zenith.discord.EmbedSerializer;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.ansi.ColorLevel;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand;

@UtilityClass
public final class ComponentSerializer {
    private static final ComponentFlattener componentFlattener = ComponentFlattener.basic().toBuilder()
        .complexMapper(TranslatableComponent.class, ComponentSerializer::translatableMapper)
        .build();
    private static final ComponentFlattener componentFlattenerWithLinks = ComponentFlattener.basic().toBuilder()
        .mapper(TextComponent.class, ComponentSerializer::linkMapper)
        .complexMapper(TranslatableComponent.class, ComponentSerializer::translatableMapper)
        .build();
    private static final ANSIComponentSerializer ansiComponentSerializer;

    static { // fixes no ansi colors being serialized on dumb terminals
        final String TERM = System.getenv("TERM"); // this should be set on unix systems
        // must be set before creating ansi serializer
        if (TERM == null) {
            String colorLevel = "indexed16";
            final String intellij = System.getenv("IDEA_INITIAL_DIRECTORY");
            final String windowsTerminal = System.getenv("WT_SESSION");
            final String cmd = System.getenv("PROMPT");
            if (intellij != null || windowsTerminal != null || cmd != null)
                colorLevel = "truecolor";
            System.setProperty(ColorLevel.COLOR_LEVEL_PROPERTY, colorLevel);
        }
        ansiComponentSerializer = ANSIComponentSerializer.builder()
            .flattener(componentFlattenerWithLinks)
            .build();
    }

    public static String serializeJson(Component component) {
        return gson().serialize(component);
    }

    public static Component deserialize(String string) {
        if (string.contains("§")) return legacyAmpersand().deserialize(string);
        return gson().deserialize(string);
    }

    public static String serializeAnsi(Component component) {
        return ansiComponentSerializer.serialize(component);
    }

    public static String serializePlainWithLinks(Component component) {
        var builder = new StringBuilder();
        componentFlattenerWithLinks.flatten(component, builder::append);
        return builder.toString();
    }

    public static String serializePlain(Component component) {
        var builder = new StringBuilder();
        componentFlattener.flatten(component, builder::append);
        return builder.toString();
    }

    public static Component minimessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static Component minimessage(String message, TagResolver... resolvers) {
        return MiniMessage.miniMessage().deserialize(message, resolvers);
    }

    public static Component deserializeEmbed(final Embed embed) {
        return EmbedSerializer.serialize(embed);
    }

    private static final Pattern TRANSLATION_CONVERTER_PATTERN = Pattern.compile("%((\\d+)\\$)?s");
    // https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html
    private MessageFormat convertToMessageFormat(String input) {
        // Escape single quotes
        String escapedInput = input.replace("'", "''");

        // Replace positional format codes like %2$s
        Matcher matcher = TRANSLATION_CONVERTER_PATTERN.matcher(escapedInput);
        StringBuilder sb = new StringBuilder();
        int lastIndex = -1;
        while (matcher.find()) {
            int argumentIndex = 0;
            // If the first group is not null, it means a number was found
            if(matcher.group(1) != null) {
                // Check if the second group exists before trying to access it
                String group2 = matcher.group(2);
                if (group2 != null) {
                    argumentIndex = Integer.parseInt(group2) - 1; // The argument index is given by the number preceding the $ sign, minus 1 because indices are 0-based
                    lastIndex = argumentIndex;
                }
            } else {
                argumentIndex = ++lastIndex; // increment lastIndex and use it
            }
            matcher.appendReplacement(sb, "{" + argumentIndex + "}");
        }
        matcher.appendTail(sb);
        return new MessageFormat(sb.toString());
    }

    private static void translatableMapper(TranslatableComponent translatableComponent, Consumer<Component> componentConsumer) {
        for (var source : GlobalTranslator.translator().sources()) {
            if (source instanceof TranslationRegistry registry && registry.contains(translatableComponent.key())) {
                componentConsumer.accept(GlobalTranslator.render(translatableComponent, Locale.ENGLISH));
                return;
            }
        }
        var fallback = translatableComponent.fallback();
        if (fallback != null) {
            for (var source : GlobalTranslator.translator().sources()) {
                if (source instanceof TranslationRegistry registry && registry.contains(fallback)) {
                    componentConsumer.accept(GlobalTranslator.render(Component.translatable(fallback), Locale.ENGLISH));
                    return;
                }
            }
        }
        if (translatableComponent.key().contains("%")) {
            var messageFormat = convertToMessageFormat(translatableComponent.key());
            var tempRegistry = TranslationRegistry.create(Key.key("zenith:zenith"));
            tempRegistry.register(translatableComponent.key(), Locale.ENGLISH, messageFormat);
            componentConsumer.accept(TranslatableComponentRenderer.usingTranslationSource(tempRegistry).render(translatableComponent, Locale.ENGLISH));
            return;
        }
    }

    private static String linkMapper(TextComponent component) {
        var content = component.content();
        if (content.isEmpty()) return "";
        if (content.startsWith("http")) return content;
        var clickEvent = component.clickEvent();
        if (clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL) {
            var link = clickEvent.value();
            return content + " (" + link + ")";
        }
        return content;
    }

}
