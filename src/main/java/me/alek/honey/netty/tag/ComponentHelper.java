package me.alek.honey.netty.tag;

import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.handler.codec.DecoderException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class ComponentHelper {

    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

    public static TextComponent readText(String page) {
        return readText(deserialize(page));
    }

    public static TextComponent readText(Component component) {
        if (!(component instanceof TextComponent)) {
            throw new DecoderException("Invalid book data: tried to read page that is not text component");
        } else {
            TextComponent text = (TextComponent) component;

            List<Component> children = new ArrayList<>();
            for (Component child : text.children()) {
                if (!(child instanceof TextComponent)) {
                    throw new DecoderException("Invalid book data: tried to read child that is not text component");
                } else {
                    TextComponent childText = (TextComponent) child;
                    if (childText.content().isEmpty()) {
                        throw new DecoderException("Invalid book data: empty child component");
                    } else if (!child.children().isEmpty()) {
                        throw new DecoderException("Invalid book data: grandchildren components");
                    } else {
                        children.add(child);
                    }
                }
            }
            // check if any of the children has been modified
            if (!children.containsAll(text.children())) {
                // apply the modified children
                return text.children(children);
            } else {
                return text;
            }
        }
    }

    public static Component deserialize(String page) {
        return GSON_COMPONENT_SERIALIZER.deserialize(page);
    }

    public static boolean isRawText(TextComponent text) {
        return text.children().isEmpty() && text.style().isEmpty();
    }

    public static String serialize(TextComponent text) {
        if (isRawText(text)) {
            return ensureOnlyDeserializableAsRawText(text.content());
        } else {
            return GSON_COMPONENT_SERIALIZER.serialize(text);
        }
    }

    public static String ensureOnlyDeserializableAsRawText(String text) {
        text = text.trim();
        if (text.startsWith("{") && text.startsWith("}")) {
            return "{text:\"" + text + "\"}";
        } else {
            return text;
        }
    }
}
