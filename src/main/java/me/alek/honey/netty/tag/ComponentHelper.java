package me.alek.honey.netty.tag;

import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.ArrayTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentHelper {

    public static TextComponent readText(String page) {
        return readText(deserialize(page));
    }

    public static TextComponent readText(Component component) {
        if (!(component instanceof TextComponent)) {
            throw new DecoderException("Invalid component: tried to read page that is not text component");
        } else {
            TextComponent text = (TextComponent) component;
            List<Component> children = new ArrayList<>();
            for (Component child : text.children()) {
                if (!(child instanceof TextComponent)) {
                    throw new DecoderException("Invalid component: tried to read child that is not text component");
                } else {
                    TextComponent childText = (TextComponent) child;
                    if (childText.content().isEmpty()) {
                        throw new DecoderException("Invalid component: empty child component");
                    } else if (!child.children().isEmpty()) {
                        throw new DecoderException("Invalid component: grandchildren components");
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

    public static Component deserialize(String json) {
        if (json.isEmpty()) {
            return Component.empty();
        }
        try {
            return AdventureSerializer.parseComponent(json);
        } catch (Exception ex) {
            throw new DecoderException("Malformed json as component", ex);
        }
    }

    public static boolean isRawText(TextComponent text) {
        return text.children().isEmpty() && text.style().isEmpty();
    }

    public static String serialize(TextComponent text) {
        if (isRawText(text)) {
            return ensureOnlyDeserializableAsRawText(text.content());
        } else {
            return AdventureSerializer.toJson(text);
        }
    }

    public static String ensureOnlyDeserializableAsRawText(String text) {
        // I hope there are no risk of json injection, I believe doing this is enough
        text = text.trim();
        if (text.startsWith("{") && text.startsWith("}")) {
            return "{text:\"" + text + "\"}";
        } else {
            return "\"" + text + "\"";
        }
    }
}
