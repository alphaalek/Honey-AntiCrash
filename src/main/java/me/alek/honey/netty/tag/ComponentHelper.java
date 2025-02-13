package me.alek.honey.netty.tag;

import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentHelper {

    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

    public static String jsonStringify(AbstractTag<?, ?, ?> tag, String emptyKey) {
        if (tag.getType() == CompoundTag.TYPE.get()) {
            CompoundTag compound = tag.asCompound();
            String inner = compound.getValue().getDelegatedMap().entrySet().stream()
                    .map(entry -> "\"" + (entry.getKey().isEmpty() ? emptyKey : entry.getKey()) + "\":" + jsonStringify(entry.getValue(), emptyKey))
                    .collect(Collectors.joining(","));
            return "{" + inner + "}";
        } else if (tag.getType().getDataTypeClass().equals(List.class)) {
            AbstractArrayTag<?, ?, ?> array = tag.asArray();
            String inner = array.getValues().stream()
                    .map(innerTag -> jsonStringify(innerTag, emptyKey))
                    .collect(Collectors.joining(","));
            return "[" + inner + "]";
        } else if (tag.getType() == TagType.ARRAY_OF_INT) {
            return "[" + Arrays.stream(tag.<int[]>asPrimitive().getValue()).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (tag.getType() == TagType.ARRAY_OF_LONG) {
            return "[" + Arrays.stream(tag.<long[]>asPrimitive().getValue()).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (tag.getType() == TagType.ARRAY_OF_BYTE) {
            byte[] val = tag.<byte[]>asPrimitive().getValue();
            return "[" + IntStream.range(0, val.length).map(i -> val[i]).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (tag.getType() == TagType.STRING) {
            return "\"" + tag.<String>asPrimitive().getValue()
                    .replaceAll("\\\\", "\\\\\\\\")
                    .replaceAll("\n", "\\\\n")
                    .replaceAll("\r", "\\\\r")
                    .replaceAll("\t", "\\\\t")
                    .replaceAll("\b", "\\\\b")
                    .replaceAll("\f", "\\\\f")
                    .replaceAll("\"", "\\\\\"")
                    + "\"";
        } else if (tag.getType() == TagType.END) {
            return "";
        } else {
            return String.valueOf(tag.asPrimitive().getValue());
        }
    }

    public static String jsonStringify(AbstractTag<?, ?, ?> tag) {
        return jsonStringify(tag, "");
    }

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
        // I hope there are no risk of json injection, I believe doing this is enough
        text = text.trim();
        if (text.startsWith("{") && text.startsWith("}")) {
            return "{text:\"" + text + "\"}";
        } else {
            return text;
        }
    }
}
