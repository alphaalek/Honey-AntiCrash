package me.alek.honey.netty.tag.nbt;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrimitiveTag<T> extends AbstractTag<T, PrimitiveTag<T>> {

    public static final PrimitiveTag<Void> END = new PrimitiveTag<>(TagType.END);

    private T value;

    public PrimitiveTag(TagType type) {
        super(type);
    }

    public PrimitiveTag(TagType type, T value) {
        this(type);
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T val) {
        this.value = val;
    }

    @Override
    public String toString() {
        if (this.type.equals(TagType.STRING)) {
            return "\"" + ((String) this.value)
                    .replaceAll("\\\\", "\\\\\\\\")
                    .replaceAll("\n", "\\\\n")
                    .replaceAll("\r", "\\\\r")
                    .replaceAll("\t", "\\\\t")
                    .replaceAll("\b", "\\\\b")
                    .replaceAll("\f", "\\\\f")
                    .replaceAll("\"", "\\\\\"")
                    + "\"";
        } else if (this.type.equals(TagType.ARRAY_OF_INT)) {
            return "[" + Arrays.stream((int[]) this.value).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (this.type.equals(TagType.ARRAY_OF_LONG)) {
            return "[" + Arrays.stream((long[]) this.value).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (this.type.equals(TagType.ARRAY_OF_BYTE)) {
            byte[] array = (byte[]) this.value;
            return "[" + IntStream.range(0, array.length).map(i -> array[i]).mapToObj(String::valueOf).collect(Collectors.joining("")) + "]";
        } else if (this.type.equals(TagType.END)) {
            return "";
        } else {
            return String.valueOf(this.value);
        }
    }
}
