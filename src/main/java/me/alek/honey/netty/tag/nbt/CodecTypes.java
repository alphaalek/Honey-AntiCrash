package me.alek.honey.netty.tag.nbt;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Primitive<Void> {

        private static final int END_BYTES = 8;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Void> tag) throws IOException {

        }

        @Override
        public PrimitiveTag<Void> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return PrimitiveTag.END
                    .incrementBytes(limiter, END_BYTES);
        }
    }

    public static class ByteCodec implements NBTCodec.Primitive<Byte> {

        private static final int BYTE_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Byte> tag) throws IOException {
            output.writeByte(tag.getValue());
        }

        @Override
        public PrimitiveTag<Byte> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.BYTE, input.readByte())
                    .incrementBytes(limiter, BYTE_BYTES);
        }
    }

    public static class ShortCodec implements NBTCodec.Primitive<Short> {

        private static final int SHORT_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Short> tag) throws IOException {
            output.writeShort(tag.getValue());
        }

        @Override
        public PrimitiveTag<Short> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.SHORT, input.readShort())
                    .incrementBytes(limiter, SHORT_BYTES);
        }
    }

    public static class IntCodec implements NBTCodec.Primitive<Integer> {

        private static final int INT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Integer> tag) throws IOException {
            output.writeInt(tag.getValue());
        }

        @Override
        public PrimitiveTag<Integer> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.INT, input.readInt())
                    .incrementBytes(limiter, INT_BYTES);
        }
    }

    public static class LongCodec implements NBTCodec.Primitive<Long> {

        private static final int LONG_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Long> tag) throws IOException {
            output.writeLong(tag.getValue());
        }

        @Override
        public PrimitiveTag<Long> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.LONG, input.readLong())
                    .incrementBytes(limiter, LONG_BYTES);
        }
    }

    public static class FloatCodec implements NBTCodec.Primitive<Float> {

        private static final int FLOAT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Float> tag) throws IOException {
            output.writeFloat(tag.getValue());
        }

        @Override
        public PrimitiveTag<Float> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.FLOAT, input.readFloat())
                    .incrementBytes(limiter, FLOAT_BYTES);
        }
    }

    public static class DoubleCodec implements NBTCodec.Primitive<Double> {

        private static final int DOUBLE_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Double> tag) throws IOException {
            output.writeDouble(tag.getValue());
        }

        @Override
        public PrimitiveTag<Double> read(DataInputStream input, TagLimiter limiter) throws IOException {
            return new PrimitiveTag<>(TagType.DOUBLE, input.readDouble())
                    .incrementBytes(limiter, DOUBLE_BYTES);
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Primitive<byte[]> {

        private static final int BYTE_ARRAY_BYTES = 24;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<byte[]> tag) throws IOException {
            if (tag.getValue() == null) {
                output.writeInt(0);
            } else {
                output.writeInt(tag.getValue().length);
                output.write(tag.getValue());
            }
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInputStream input, TagLimiter limiter) throws IOException {
            int length = input.readInt();
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readByte();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, array)
                    .incrementBytes(limiter, BYTE_ARRAY_BYTES)
                    .incrementBytes(limiter, length);
        }
    }

    public static class StringCodec implements NBTCodec.Primitive<String> {

        private static final int STRING_BYTES = 36;
        private static final int CHAR_BYTES = 2;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<String> tag) throws IOException {
            output.writeUTF(tag.getValue());
        }

        @Override
        public PrimitiveTag<String> read(DataInputStream input, TagLimiter limiter) throws IOException {
            String str = input.readUTF();
            return new PrimitiveTag<>(TagType.STRING, str)
                    .incrementBytes(limiter, STRING_BYTES)
                    .incrementBytes(limiter, getBytesForLimiter(str));
        }

        private static int getBytesForLimiter(String str) {
            return Math.max(str.length() * CHAR_BYTES, getBytesFor(str));
        }

        private static int getBytesFor(String str) {
            return str.codePoints().map(operand -> getBytesFor((char) operand)).sum();
        }

        private static int getBytesFor(char c) {
            return (Integer.SIZE >> 3) - (Integer.numberOfLeadingZeros(c) >> 3);
        }
    }

    public static class LongArrayCodec implements NBTCodec.Primitive<long[]> {

        private static final int LONG_ARRAY_BYTES = 24;
        private static final int LONG_ARRAY_ELEMENT_BYTES = 8;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<long[]> tag) throws IOException {
            if (tag.getValue() == null) {
                output.writeInt(0);
            } else {
                output.writeInt(tag.getValue().length);
                for (long value : tag.getValue()) {
                    output.writeLong(value);
                }
            }
        }

        @Override
        public PrimitiveTag<long[]> read(DataInputStream input, TagLimiter limiter) throws IOException {
            int length = input.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readLong();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, array)
                    .incrementBytes(limiter, LONG_ARRAY_BYTES)
                    .incrementBytes(limiter, length * LONG_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Primitive<int[]> {

        private static final int INT_ARRAY_BYTES = 24;
        private static final int INT_ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<int[]> tag) throws IOException {
            if (tag.getValue() == null) {
                output.writeInt(0);
            } else {
                output.writeInt(tag.getValue().length);
                for (int value : tag.getValue()) {
                    output.writeInt(value);
                }
            }
        }

        @Override
        public PrimitiveTag<int[]> read(DataInputStream input, TagLimiter limiter) throws IOException {
            int length = input.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readInt();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, array)
                    .incrementBytes(limiter, INT_ARRAY_BYTES)
                    .incrementBytes(limiter, length * INT_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class ArrayCodec<T extends AbstractTag<?, ?, T>, R extends AbstractArrayTag<T, R, ?>> implements NBTCodec<R> {

        private static final int ARRAY_BYTES = 37;
        private static final int ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, R array) throws IOException {
            if (array.getValue().isEmpty()) {
                output.writeByte(TagType.END.getTypeId());
            } else {
                output.writeByte(Optional.ofNullable(array.getTypeOf()).map(TagType::getTypeId).orElseThrow(IllegalStateException::new));
            }
            output.writeInt(array.getValue().size());
            for (AbstractTag<?, ?, ?> tag : array.getValue()) {
                tag.write(output);
            }
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public R read(DataInputStream input, TagLimiter limiter) throws IOException {
            TagType type = TagType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException("Invalid type");
            }
            int depth = limiter.incrementNestingDepth();

            int length = input.readInt();
            List<T> array = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                array.add(type.<T>getCodec().read(input, limiter));
            }

            limiter.setNestingDepthLevel(depth);

            R wrappedArray;
            if (type.hasCollection()) {
                switch (type.getCollection()) {
                    case ARRAY:
                        wrappedArray = (R) createArrayOfArray(array);
                        break;
                    case COMPOUND:
                        wrappedArray = (R) new GeneticArrayTypes.ArrayOfCompoundTag(array);
                        break;
                    default:
                        throw new DecoderException("Wrong array type");
                }
            } else {
                wrappedArray = (R) new ArrayTag<>(type, array);
            }

            return wrappedArray
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, ARRAY_BYTES)
                    .incrementBytes(limiter, ARRAY_ELEMENT_BYTES * wrappedArray.size());
        }

        private <ARRAY extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>, IN extends AbstractArrayTag<C, IN, ?>, C extends AbstractTag<?, ?, C>> GeneticArrayTypes.ArrayOfArrayTag<?, IN, C> createArrayOfArray(Iterable<T> array) {
            return new GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>(array);
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        private static final int COMPOUND_BYTES = 48;
        private static final int COMPOUND_ELEMENT_BYTES = 36;

        @Override
        public void write(DataOutputStream output, CompoundTag compound) throws IOException {
            for (String key : compound.getValue().keySet()) {
                AbstractTag<?, ?, ?> tag = compound.getValue().getConverted(key);
                if (tag.getType() == TagType.END) {
                    continue;
                }
                output.writeByte(tag.getType().getTypeId());

                output.writeUTF(key);
                tag.write(output);
            }

            output.writeByte(TagType.END.getTypeId());
        }

        @Override
        public CompoundTag read(DataInputStream input, TagLimiter limiter) throws IOException {
            Map<String, Object> map = new HashMap<>();

            int depth = limiter.incrementNestingDepth();
            byte b;
            while ((b = input.readByte()) != 0) {
                TagType type = TagType.getFromTypeId(b);
                if (type == null) {
                    throw new IOException("Invalid type");
                }
                if (type == TagType.END) {
                    continue;
                }

                String key = input.readUTF();
                Object value = type.getCodec().read(input, limiter);
                if (value != null) {
                    map.put(key, value);
                }
            }

            limiter.setNestingDepthLevel(depth);

            return new CompoundTag(map)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, map.keySet().stream().mapToInt(StringCodec::getBytesForLimiter).sum())
                    .incrementBytes(limiter, COMPOUND_BYTES)
                    .incrementBytes(limiter, COMPOUND_ELEMENT_BYTES * map.size());
        }
    }
}
