package me.alek.honey.netty.tag.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.types.Utf8String;
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
        public void write(DataOutputStream output, PrimitiveTag<Void> tag, TagVisitor.Out visitor) throws IOException {
            visitor.visitEnd();
        }

        @Override
        public PrimitiveTag<Void> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            visitor.visitAtEnd(input, limiter);
            visitor.visitEnd();
            return PrimitiveTag.END
                    .increment(limiter)
                    .incrementBytes(limiter, END_BYTES);
        }
    }

    public static class ByteCodec implements NBTCodec.Primitive<Byte> {

        private static final int BYTE_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Byte> tag, TagVisitor.Out visitor) throws IOException {
            byte val = tag.getValue();
            output.writeByte(val);
            visitor.visitByte(val);
        }

        @Override
        public PrimitiveTag<Byte> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            byte val;
            try {
                val = visitor.visitAtByte(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readByte();
            }
            visitor.visitByte(val);
            return new PrimitiveTag<>(TagType.BYTE, val)
                    .increment(limiter)
                    .incrementBytes(limiter, BYTE_BYTES);
        }
    }

    public static class ShortCodec implements NBTCodec.Primitive<Short> {

        private static final int SHORT_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Short> tag, TagVisitor.Out visitor) throws IOException {
            short val = tag.getValue();
            output.writeShort(val);
            visitor.visitShort(val);
        }

        @Override
        public PrimitiveTag<Short> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            short val;
            try {
                val = visitor.visitAtShort(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readShort();
            }
            visitor.visitShort(val);
            return new PrimitiveTag<>(TagType.SHORT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, SHORT_BYTES);
        }
    }

    public static class IntCodec implements NBTCodec.Primitive<Integer> {

        private static final int INT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Integer> tag, TagVisitor.Out visitor) throws IOException {
            int val = tag.getValue();
            output.writeInt(val);
            visitor.visitInt(val);
        }

        @Override
        public PrimitiveTag<Integer> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            int val;
            try {
                val = visitor.visitAtInt(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readInt();
            }
            visitor.visitInt(val);
            return new PrimitiveTag<>(TagType.INT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, INT_BYTES);
        }
    }

    public static class LongCodec implements NBTCodec.Primitive<Long> {

        private static final int LONG_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Long> tag, TagVisitor.Out visitor) throws IOException {
            long val = tag.getValue();
            output.writeLong(val);
            visitor.visitLong(val);
        }

        @Override
        public PrimitiveTag<Long> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            long val;
            try {
                val = visitor.visitAtLong(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readLong();
            }
            visitor.visitLong(val);
            return new PrimitiveTag<>(TagType.LONG, val)
                    .increment(limiter)
                    .incrementBytes(limiter, LONG_BYTES);
        }
    }

    public static class FloatCodec implements NBTCodec.Primitive<Float> {

        private static final int FLOAT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Float> tag, TagVisitor.Out visitor) throws IOException {
            float val = tag.getValue();
            output.writeFloat(val);
            visitor.visitFloat(val);
        }

        @Override
        public PrimitiveTag<Float> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            float val;
            try {
                val = visitor.visitAtFloat(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readFloat();
            }
            visitor.visitFloat(val);
            return new PrimitiveTag<>(TagType.FLOAT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, FLOAT_BYTES);
        }
    }

    public static class DoubleCodec implements NBTCodec.Primitive<Double> {

        private static final int DOUBLE_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Double> tag, TagVisitor.Out visitor) throws IOException {
            double val = tag.getValue();
            output.writeDouble(tag.getValue());
            visitor.visitDouble(val);
        }

        @Override
        public PrimitiveTag<Double> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            double val;
            try {
                val = visitor.visitAtDouble(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException ex) {
                val = input.readDouble();
            }
            visitor.visitDouble(val);
            return new PrimitiveTag<>(TagType.DOUBLE, val)
                    .increment(limiter)
                    .incrementBytes(limiter, DOUBLE_BYTES);
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Primitive<byte[]> {

        private static final int BYTE_ARRAY_BYTES = 24;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<byte[]> tag, TagVisitor.Out visitor) throws IOException {
            byte[] array = tag.getValue();
            output.writeInt(array.length);
            output.write(array);
            visitor.visitByteArray(array);
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            byte[] array;
            try {
                array = visitor.visitAtByteArray(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new byte[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readByte();
                }
            }
            visitor.visitByteArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, array)
                    .increment(limiter)
                    .incrementBytes(limiter, BYTE_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length);
        }
    }

    public static class StringCodec implements NBTCodec.Primitive<String> {

        private static final int STRING_BYTES = 36;
        private static final int CHAR_BYTES = 2;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<String> tag, TagVisitor.Out visitor) throws IOException {
            String str = tag.getValue();
            output.writeUTF(str);
            visitor.visitString(str);
        }

        @Override
        public PrimitiveTag<String> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            String str;
            try {
                str = visitor.visitAtString(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                if (limiter instanceof TagLimiter.UTF8Limiter) {
                    str = ((TagLimiter.UTF8Limiter)limiter).readString(input);
                } else {
                    str = Utf8String.readUTFInputStream(input, ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH);
                }
            }
            visitor.visitString(str);
            return new PrimitiveTag<>(TagType.STRING, str)
                    .increment(limiter)
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
        public void write(DataOutputStream output, PrimitiveTag<long[]> tag, TagVisitor.Out visitor) throws IOException {
            long[] array = tag.getValue();
            output.writeInt(array.length);
            for (long value : array) {
                output.writeLong(value);
            }
            visitor.visitLongArray(array);
        }

        @Override
        public PrimitiveTag<long[]> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            long[] array;
            try {
                array = visitor.visitAtLongArray(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new long[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readLong();
                }
            }
            visitor.visitLongArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, array)
                    .increment(limiter)
                    .incrementBytes(limiter, LONG_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length * LONG_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Primitive<int[]> {

        private static final int INT_ARRAY_BYTES = 24;
        private static final int INT_ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<int[]> tag, TagVisitor.Out visitor) throws IOException {
            int[] array = tag.getValue();
            output.writeInt(array.length);
            for (int value : array) {
                output.writeInt(value);
            }
            visitor.visitIntArray(array);
        }

        @Override
        public PrimitiveTag<int[]> read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            int[] array;
            try {
                array = visitor.visitAtIntArray(input, limiter);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readInt();
                }
            }
            visitor.visitIntArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, array)
                    .increment(limiter)
                    .incrementBytes(limiter, INT_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length * INT_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class ArrayCodec<T extends AbstractTag<?, ?, T>, R extends AbstractArrayTag<T, R, ?>> implements NBTCodec<R> {

        private static final int ARRAY_BYTES = 37;
        private static final int ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, R arrayTag, TagVisitor.Out visitor) throws IOException {
            List<T> array = arrayTag.getValues();
            TagType type = arrayTag.getTypeOf();

            output.writeByte(type.getId());
            output.writeInt(array.size());
            for (T tag : array) {
                try {
                    tag = visitor.visitAtArrayFlush(output, tag);
                    if (!tag.getType().equals(type)) {
                        throw new IOException("Illegal type");
                    }
                } catch (TagVisitor.FallbackDefaultCodecException ignored) {
                    // to do nothing
                }
                visitor.visitArrayElement(tag);
                tag.write(output, visitor);
            }
            visitor.visitArray(array, type);
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public R read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            TagType type = TagType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException("Invalid type");
            }
            List<T> array;
            int depth = limiter.incrementNestingDepth();
            try {
                array = visitor.visitAtArray(input, limiter, type);
                for (T val : array) {
                    if (!val.getType().equals(type)) {
                        throw new IOException("Illegal type");
                    }
                    visitor.visitArrayElement(val);
                }
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                array = readArray(type, input, limiter, visitor);
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
                        throw new IOException("Wrong array type");
                }
            } else {
                wrappedArray = (R) new ArrayTag<>(type, array);
            }

            visitor.visitArray(array, type);
            return wrappedArray
                    .increment(limiter)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, ARRAY_BYTES)
                    .incrementBytes(limiter, ARRAY_ELEMENT_BYTES * wrappedArray.size());
        }

        private List<T> readArray(TagType type, DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            List<T> array = new ArrayList<>();
            int length = input.readInt();
            for (int i = 0; i < length; i++) {
                T val;
                try {
                    val = visitor.visitAtArrayInsert(input, limiter, type);
                } catch (TagVisitor.FallbackDefaultCodecException ex) {
                    val = type.<T>getCodec().read(input, limiter, visitor);
                }
                array.add(val);
                visitor.visitArrayElement(val);
            }
            return array;
        }

        private <ARRAY extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>, IN extends AbstractArrayTag<C, IN, ?>, C extends AbstractTag<?, ?, C>> GeneticArrayTypes.ArrayOfArrayTag<?, IN, C> createArrayOfArray(Iterable<T> array) {
            return new GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>(array);
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        private static final int COMPOUND_BYTES = 48;
        private static final int COMPOUND_ELEMENT_BYTES = 36;

        @Override
        public void write(DataOutputStream output, CompoundTag compoundTag, TagVisitor.Out visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound = compoundTag.getValue().getDelegatedMap();
            for (Map.Entry<String, AbstractTag<?, ?, ?>> entry : compound.entrySet()) {
                if (entry.getValue().getType() == TagType.END) {
                    continue;
                }
                try {
                    entry = visitor.visitAtCompoundFlush(output, entry.getKey(), entry.getValue());
                    if (entry.getValue().getType() == TagType.END) {
                        throw new IOException("Illegal type");
                    }
                } catch (TagVisitor.FallbackDefaultCodecException ignored) {
                    // to do nothing
                }

                String key = entry.getKey();
                AbstractTag<?, ?, ?> tag = entry.getValue();
                visitor.visitCompoundElement(key, tag);

                output.writeByte(tag.getType().getId());
                output.writeUTF(key);
                tag.write(output, visitor);
            }
            visitor.visitCompound(compound);
            output.writeByte(TagType.END.getId());
        }

        @Override
        public CompoundTag read(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound;
            int depth = limiter.incrementNestingDepth();
            try {
                compound = visitor.visitAtCompound(input, limiter);
                for (Map.Entry<String, AbstractTag<?, ?, ?>> entry : compound.entrySet()) {
                    AbstractTag<?, ?, ?> tag = entry.getValue();
                    if (tag.getType().equals(TagType.END)) {
                        throw new IOException("Illegal type");
                    }
                    visitor.visitCompoundElement(entry.getKey(), entry.getValue());
                }
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                compound = readComponent(input, limiter, visitor);
            }
            limiter.setNestingDepthLevel(depth);

            visitor.visitCompound(compound);
            return new CompoundTag(compound)
                    .increment(limiter)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, compound.keySet().stream().mapToInt(StringCodec::getBytesForLimiter).sum())
                    .incrementBytes(limiter, COMPOUND_BYTES)
                    .incrementBytes(limiter, COMPOUND_ELEMENT_BYTES * compound.size());
        }

        private Map<String, AbstractTag<?, ?, ?>> readComponent(DataInputStream input, TagLimiter limiter, TagVisitor.In visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound = new HashMap<>();
            byte b;
            while ((b = input.readByte()) != TagType.END.getId()) {
                TagType type = TagType.getFromTypeId(b);
                if (type == null) {
                    throw new IOException("Invalid type");
                }

                String key;
                AbstractTag<?, ?, ?> tag;
                try {
                    Map.Entry<String, AbstractTag<?, ?, ?>> entry = visitor.visitAtCompoundInsert(input, type);
                    key = entry.getKey();
                    tag = entry.getValue();
                } catch (TagVisitor.FallbackDefaultCodecException ex) {
                    try {
                        key = visitor.visitAtCompoundInsertKey(input, type);
                    } catch (TagVisitor.FallbackDefaultCodecException ex2) {
                        key = Utf8String.readUTFInputStream(input, ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_KEY_LENGTH);
                    }
                    try {
                        tag = visitor.visitAtCompoundInsertTag(input, limiter, type, key);
                    } catch (TagVisitor.FallbackDefaultCodecException ex2) {
                        tag = type.getCodec().read(input, limiter, visitor);
                    }
                }

                key = visitor.visitCompoundKey(key);
                compound.put(key, tag);
                visitor.visitCompoundElement(key, tag);
            }
            return compound;
        }
    }
}
