package me.alek.honey.netty.tag.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.types.Utf8String;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Primitive<Void> {

        private static final int END_BYTES = 1;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Void> tag, NBTVisitor.Out visitor) throws IOException {
            visitor.visitEnd();
        }

        @Override
        public PrimitiveTag<Void> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            visitor.visitAtEnd(input, limiter);
            visitor.visitEnd();
            return PrimitiveTag.END
                    .increment(limiter)
                    .incrementBytes(limiter, END_BYTES);
        }
    }

    public static class ByteCodec implements NBTCodec.Primitive<Byte> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Byte> tag, NBTVisitor.Out visitor) throws IOException {
            byte val = tag.getValue();
            output.writeByte(val);
            visitor.visitByte(val);
        }

        @Override
        public PrimitiveTag<Byte> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            byte val;
            try {
                val = visitor.visitAtByte(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                val = input.readByte();
            }
            visitor.visitByte(val);
            return new PrimitiveTag<>(TagType.BYTE, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Byte.BYTES);
        }
    }

    public static class ShortCodec implements NBTCodec.Primitive<Short> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Short> tag, NBTVisitor.Out visitor) throws IOException {
            short val = tag.getValue();
            output.writeShort(val);
            visitor.visitShort(val);
        }

        @Override
        public PrimitiveTag<Short> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            short val;
            try {
                val = visitor.visitAtShort(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                val = input.readShort();
            }
            visitor.visitShort(val);
            return new PrimitiveTag<>(TagType.SHORT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Short.BYTES);
        }
    }

    public static class IntCodec implements NBTCodec.Primitive<Integer> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Integer> tag, NBTVisitor.Out visitor) throws IOException {
            int val = tag.getValue();
            output.writeInt(val);
            visitor.visitInt(val);
        }

        @Override
        public PrimitiveTag<Integer> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            int val;
            try {
                val = visitor.visitAtInt(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                val = input.readInt();
            }
            visitor.visitInt(val);
            return new PrimitiveTag<>(TagType.INT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Integer.BYTES);
        }
    }

    public static class LongCodec implements NBTCodec.Primitive<Long> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Long> tag, NBTVisitor.Out visitor) throws IOException {
            long val = tag.getValue();
            output.writeLong(val);
            visitor.visitLong(val);
        }

        @Override
        public PrimitiveTag<Long> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            long val;
            try {
                val = visitor.visitAtLong(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                val = input.readLong();
            }
            visitor.visitLong(val);
            return new PrimitiveTag<>(TagType.LONG, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Long.BYTES);
        }
    }

    public static class FloatCodec implements NBTCodec.Primitive<Float> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Float> tag, NBTVisitor.Out visitor) throws IOException {
            float val = tag.getValue();
            output.writeFloat(val);
            visitor.visitFloat(val);
        }

        @Override
        public PrimitiveTag<Float> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            float val;
            try {
                val = visitor.visitAtFloat(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                val = input.readFloat();
            }
            visitor.visitFloat(val);
            return new PrimitiveTag<>(TagType.FLOAT, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Float.BYTES);
        }
    }

    public static class DoubleCodec implements NBTCodec.Primitive<Double> {

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Double> tag, NBTVisitor.Out visitor) throws IOException {
            double val = tag.getValue();
            output.writeDouble(tag.getValue());
            visitor.visitDouble(val);
        }

        @Override
        public PrimitiveTag<Double> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            double val;
            try {
                val = visitor.visitAtDouble(input, limiter);
            } catch (FallbackDefaultCodecException ex) {
                val = input.readDouble();
            }
            visitor.visitDouble(val);
            return new PrimitiveTag<>(TagType.DOUBLE, val)
                    .increment(limiter)
                    .incrementBytes(limiter, Double.BYTES);
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Primitive<byte[]> {

        private static final int BYTE_ARRAY_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<byte[]> tag, NBTVisitor.Out visitor) throws IOException {
            byte[] array = tag.getValue();
            output.writeInt(array.length);
            output.write(array);
            visitor.visitByteArray(array);
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            byte[] array;
            try {
                array = visitor.visitAtByteArray(input, limiter);
            } catch (FallbackDefaultCodecException e) {
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
                    .incrementBytes(limiter, array.length * Byte.BYTES);
        }
    }

    public static class StringCodec implements NBTCodec.Primitive<String> {

        private static final int STRING_BYTES = 8;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<String> tag, NBTVisitor.Out visitor) throws IOException {
            String str = tag.getValue();
            output.writeUTF(str);
            visitor.visitString(str);
        }

        @Override
        public PrimitiveTag<String> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            String str;
            try {
                str = visitor.visitAtString(input, limiter);
            } catch (FallbackDefaultCodecException e) {
                if (limiter instanceof TagLimiter.UTF8Limiter) {
                    str = ((TagLimiter.UTF8Limiter) limiter).readString(input);
                } else {
                    str = Utf8String.readUTFInputStream(input, ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH);
                }
            }
            visitor.visitString(str);
            return new PrimitiveTag<>(TagType.STRING, str)
                    .increment(limiter)
                    .incrementBytes(limiter, STRING_BYTES)
                    .incrementBytes(limiter, getBytesFor(str));
        }

        public static int getBytesFor(String str) {
            return str.codePoints().map(i -> getBytesFor((char) i)).sum();
        }

        public static int getBytesFor(char c) {
            return Integer.BYTES - (Integer.numberOfLeadingZeros(c) >> 3);
        }
    }

    public static class LongArrayCodec implements NBTCodec.Primitive<long[]> {

        private static final int LONG_ARRAY_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<long[]> tag, NBTVisitor.Out visitor) throws IOException {
            long[] array = tag.getValue();
            output.writeInt(array.length);
            for (long value : array) {
                output.writeLong(value);
            }
            visitor.visitLongArray(array);
        }

        @Override
        public PrimitiveTag<long[]> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            long[] array;
            try {
                array = visitor.visitAtLongArray(input, limiter);
            } catch (FallbackDefaultCodecException e) {
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
                    .incrementBytes(limiter, array.length * Long.BYTES);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Primitive<int[]> {

        private static final int INT_ARRAY_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<int[]> tag, NBTVisitor.Out visitor) throws IOException {
            int[] array = tag.getValue();
            output.writeInt(array.length);
            for (int value : array) {
                output.writeInt(value);
            }
            visitor.visitIntArray(array);
        }

        @Override
        public PrimitiveTag<int[]> read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            int[] array;
            try {
                array = visitor.visitAtIntArray(input, limiter);
            } catch (FallbackDefaultCodecException e) {
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
                    .incrementBytes(limiter, array.length * Integer.BYTES);
        }
    }

    public static class ArrayCodec<T extends AbstractTag<?, T>, R extends ArrayTag<T, ?, R>> implements NBTCodec<R> {

        private static final int ARRAY_BYTES = 24;
        private static final int ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, R arrayTag, NBTVisitor.Out visitor) throws IOException {
            List<T> array = arrayTag.getTags();
            TagType type = arrayTag.getTypeOf();

            output.writeByte(type.getId());
            output.writeInt(array.size());
            for (T tag : array) {
                try {
                    tag = visitor.visitAtArrayFlush(output, tag);
                    if (!tag.getType().equals(type)) {
                        throw new IOException("Illegal type");
                    }
                } catch (FallbackDefaultCodecException ignored) {
                    // to do nothing
                }
                visitor.visitArrayElement(tag);
                tag.write(output, visitor);
            }
            visitor.visitArray(array, type);
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public R read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
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
            } catch (FallbackDefaultCodecException e) {
                array = readArray(type, input, limiter, visitor);
            }
            limiter.setNestingDepthLevel(depth);

            R wrappedArray;
            if (type == CompoundTag.TYPE.get() || type == ArrayTag.TYPE.get()) {
                wrappedArray = (R) new ArrayTag<>(type, array);
            } else {
                wrappedArray = (R) new PrimitiveArrayTag<>(type, array);
            }

            visitor.visitArray(array, type);
            return wrappedArray
                    .increment(limiter)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, ARRAY_BYTES)
                    .incrementBytes(limiter, ARRAY_ELEMENT_BYTES * array.size());
        }

        private List<T> readArray(TagType type, DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            List<T> array = new ArrayList<>();
            int length = input.readInt();
            for (int i = 0; i < length; i++) {
                T val;
                try {
                    val = visitor.visitAtArrayInsert(input, limiter, type);
                } catch (FallbackDefaultCodecException ex) {
                    val = type.<T>getCodec().read(input, limiter, visitor);
                }
                array.add(val);
                visitor.visitArrayElement(val);
            }
            return array;
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        private static final int COMPOUND_BYTES = 32;
        private static final int COMPOUND_ELEMENT_BYTES = 8;

        @Override
        public void write(DataOutputStream output, CompoundTag compound, NBTVisitor.Out visitor) throws IOException {
            for (Map.Entry<String, AbstractTag<?, ?>> entry : compound.entrySet()) {
                if (entry.getValue().getType() == TagType.END) {
                    continue;
                }
                try {
                    entry = visitor.visitAtCompoundFlush(output, entry.getKey(), entry.getValue());
                    if (entry.getValue().getType() == TagType.END) {
                        throw new IOException("Illegal type");
                    }
                } catch (FallbackDefaultCodecException ignored) {
                    // to do nothing
                }

                String key = entry.getKey();
                AbstractTag<?, ?> tag = entry.getValue();
                visitor.visitCompoundElement(key, tag);

                output.writeByte(tag.getType().getId());
                output.writeUTF(key);
                tag.write(output, visitor);
            }
            visitor.visitCompound(compound.getDelegate());
            output.writeByte(TagType.END.getId());
        }

        @Override
        public CompoundTag read(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            Map<String, AbstractTag<?, ?>> compound;
            int depth = limiter.incrementNestingDepth();
            try {
                compound = visitor.visitAtCompound(input, limiter);
                for (Map.Entry<String, AbstractTag<?, ?>> entry : compound.entrySet()) {
                    AbstractTag<?, ?> tag = entry.getValue();
                    if (tag.getType().equals(TagType.END)) {
                        throw new IOException("Illegal type");
                    }
                    visitor.visitCompoundElement(entry.getKey(), entry.getValue());
                }
            } catch (FallbackDefaultCodecException e) {
                compound = readComponent(input, limiter, visitor);
            }
            limiter.setNestingDepthLevel(depth);

            visitor.visitCompound(compound);
            return new CompoundTag(compound)
                    .increment(limiter)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, compound.keySet().stream().mapToInt(StringCodec::getBytesFor).sum())
                    .incrementBytes(limiter, COMPOUND_BYTES)
                    .incrementBytes(limiter, COMPOUND_ELEMENT_BYTES * (compound.size() + 1));
        }

        private Map<String, AbstractTag<?, ?>> readComponent(DataInputStream input, TagLimiter limiter, NBTVisitor.In visitor) throws IOException {
            Map<String, AbstractTag<?, ?>> compound = new HashMap<>();
            byte b;
            while ((b = input.readByte()) != TagType.END.getId()) {
                TagType type = TagType.getFromTypeId(b);
                if (type == null) {
                    throw new IOException("Invalid type");
                }

                String key;
                AbstractTag<?, ?> tag;
                try {
                    Map.Entry<String, AbstractTag<?, ?>> entry = visitor.visitAtCompoundInsert(input, type);
                    key = entry.getKey();
                    tag = entry.getValue();
                } catch (FallbackDefaultCodecException ex) {
                    try {
                        key = visitor.visitAtCompoundInsertKey(input, type);
                    } catch (FallbackDefaultCodecException ex2) {
                        key = Utf8String.readUTFInputStream(input, ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_KEY_LENGTH);
                    }
                    try {
                        tag = visitor.visitAtCompoundInsertTag(input, limiter, type, key);
                    } catch (FallbackDefaultCodecException ex2) {
                        tag = type.getCodec().read(input, limiter, visitor);
                    }
                }

                key = visitor.visitCompoundKey(key, type);
                compound.put(key, tag);
                visitor.visitCompoundElement(key, tag);
            }
            return compound;
        }
    }
}