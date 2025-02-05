package me.alek.honey.netty.tag.nbt;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Primitive<Void> {

        private static final int END_BYTES = 8;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Void> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            visitor.visitAtEnd(output);
            visitor.visitEnd();
        }

        @Override
        public PrimitiveTag<Void> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtEnd(input);
            visitor.visitEnd();
            return PrimitiveTag.END
                    .incrementBytes(limiter, END_BYTES);
        }
    }

    public static class ByteCodec implements NBTCodec.Primitive<Byte> {

        private static final int BYTE_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Byte> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            byte val;
            try {
                val = visitor.visitAtByte(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeByte(val);
            }
            visitor.visitByte(val);
        }

        @Override
        public PrimitiveTag<Byte> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            byte val;
            try {
                val = visitor.visitAtByte(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readByte();
            }
            visitor.visitByte(val);
            return new PrimitiveTag<>(TagType.BYTE, val)
                    .incrementBytes(limiter, BYTE_BYTES);
        }
    }

    public static class ShortCodec implements NBTCodec.Primitive<Short> {

        private static final int SHORT_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Short> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            short val;
            try {
                val = visitor.visitAtShort(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeShort(val);
            }
            visitor.visitShort(val);
        }

        @Override
        public PrimitiveTag<Short> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            short val;
            try {
                val = visitor.visitAtShort(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readShort();
            }
            visitor.visitShort(val);
            return new PrimitiveTag<>(TagType.SHORT, val)
                    .incrementBytes(limiter, SHORT_BYTES);
        }
    }

    public static class IntCodec implements NBTCodec.Primitive<Integer> {

        private static final int INT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Integer> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            int val;
            try {
                val = visitor.visitAtInt(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeInt(val);
            }
            visitor.visitInt(val);
        }

        @Override
        public PrimitiveTag<Integer> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            int val;
            try {
                val = visitor.visitAtInt(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readInt();
            }
            visitor.visitInt(val);
            return new PrimitiveTag<>(TagType.INT, val)
                    .incrementBytes(limiter, INT_BYTES);
        }
    }

    public static class LongCodec implements NBTCodec.Primitive<Long> {

        private static final int LONG_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Long> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            long val;
            try {
                val = visitor.visitAtLong(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeLong(val);
            }
            visitor.visitLong(val);
        }

        @Override
        public PrimitiveTag<Long> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            long val;
            try {
                val = visitor.visitAtLong(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readLong();
            }
            visitor.visitLong(val);
            return new PrimitiveTag<>(TagType.LONG, val)
                    .incrementBytes(limiter, LONG_BYTES);
        }
    }

    public static class FloatCodec implements NBTCodec.Primitive<Float> {

        private static final int FLOAT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Float> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            float val;
            try {
                val = visitor.visitAtFloat(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeFloat(val);
            }
            visitor.visitFloat(val);
        }

        @Override
        public PrimitiveTag<Float> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            float val;
            try {
                val = visitor.visitAtFloat(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readFloat();
            }
            visitor.visitFloat(val);
            return new PrimitiveTag<>(TagType.FLOAT, val)
                    .incrementBytes(limiter, FLOAT_BYTES);
        }
    }

    public static class DoubleCodec implements NBTCodec.Primitive<Double> {

        private static final int DOUBLE_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Double> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            double val;
            try {
                val = visitor.visitAtDouble(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = tag.getValue();
                output.writeDouble(tag.getValue());
            }
            visitor.visitDouble(val);
        }

        @Override
        public PrimitiveTag<Double> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            double val;
            try {
                val = visitor.visitAtDouble(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                val = input.readDouble();
            }
            visitor.visitDouble(val);
            return new PrimitiveTag<>(TagType.DOUBLE, val)
                    .incrementBytes(limiter, DOUBLE_BYTES);
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Primitive<byte[]> {

        private static final int BYTE_ARRAY_BYTES = 24;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<byte[]> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            byte[] array;
            try {
                array = visitor.visitAtByteArray(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                array = tag.getValue();
                output.writeInt(array.length);
                output.write(array);
            }
            visitor.visitByteArray(array);
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            byte[] array;
            try {
                array = visitor.visitAtByteArray(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new byte[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readByte();
                }
            }
            visitor.visitByteArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, array)
                    .incrementBytes(limiter, BYTE_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length);
        }
    }

    public static class StringCodec implements NBTCodec.Primitive<String> {

        private static final int STRING_BYTES = 36;
        private static final int CHAR_BYTES = 2;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<String> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            String str;
            try {
                str = visitor.visitAtString(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                str = tag.getValue();
                output.writeUTF(str);
            }
            visitor.visitString(str);
        }

        @Override
        public PrimitiveTag<String> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            String str;
            try {
                str = visitor.visitAtString(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                str = input.readUTF();
            }
            visitor.visitString(str);
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
        public void write(DataOutputStream output, PrimitiveTag<long[]> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            long[] array;
            try {
                array = visitor.visitAtLongArray(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                array = tag.getValue();
                output.writeInt(array.length);
                for (long value : array) {
                    output.writeLong(value);
                }
            }
            visitor.visitLongArray(array);
        }

        @Override
        public PrimitiveTag<long[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            long[] array;
            try {
                array = visitor.visitAtLongArray(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new long[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readLong();
                }
            }
            visitor.visitLongArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, array)
                    .incrementBytes(limiter, LONG_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length * LONG_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Primitive<int[]> {

        private static final int INT_ARRAY_BYTES = 24;
        private static final int INT_ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<int[]> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            int[] array;
            try {
                array = visitor.visitAtIntArray(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                array = tag.getValue();
                output.writeInt(array.length);
                for (int value : array) {
                    output.writeInt(value);
                }
            }
            visitor.visitIntArray(array);
        }

        @Override
        public PrimitiveTag<int[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            int[] array;
            try {
                array = visitor.visitAtIntArray(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                int length = input.readInt();
                array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = input.readInt();
                }
            }
            visitor.visitIntArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, array)
                    .incrementBytes(limiter, INT_ARRAY_BYTES)
                    .incrementBytes(limiter, array.length * INT_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class ArrayCodec<T extends AbstractTag<?, ?, T>, R extends AbstractArrayTag<T, R, ?>> implements NBTCodec<R> {

        private static final int ARRAY_BYTES = 37;
        private static final int ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, R array, TagVisitor<DataOutputStream> visitor) throws IOException {
            List<T> list;
            try {
                list = visitor.visitAtArray(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                list = array.getValues();
                if (list.isEmpty()) {
                    output.writeByte(TagType.END.getTypeId());
                } else {
                    output.writeByte(Optional.ofNullable(array.getTypeOf()).map(TagType::getTypeId).orElseThrow(IllegalStateException::new));
                }
                output.writeInt(array.getValue().size());
                for (AbstractTag<?, ?, ?> tag : array.getValue()) {
                    tag.write(output, visitor);
                }
            }
            visitor.visitArray(list);
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public R read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            List<T> array;
            TagType type;
            int depth = limiter.incrementNestingDepth();
            try {
                array = visitor.visitAtArray(input);
                if (array.isEmpty()) {
                    type = TagType.END;
                } else {
                    type = TagType.getFromClass(array.get(0).getClass());
                }
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                array = new ArrayList<>();
                type = readArray(array, input, limiter, visitor);
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

            visitor.visitArray(array);
            return wrappedArray
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, ARRAY_BYTES)
                    .incrementBytes(limiter, ARRAY_ELEMENT_BYTES * wrappedArray.size());
        }

        private TagType readArray(List<T> array, DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            TagType type = TagType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException("Invalid type");
            }
            int length = input.readInt();
            for (int i = 0; i < length; i++) {
                array.add(type.<T>getCodec().read(input, limiter, visitor));
            }
            return type;
        }

        private <ARRAY extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>, IN extends AbstractArrayTag<C, IN, ?>, C extends AbstractTag<?, ?, C>> GeneticArrayTypes.ArrayOfArrayTag<?, IN, C> createArrayOfArray(Iterable<T> array) {
            return new GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>(array);
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        private static final int COMPOUND_BYTES = 48;
        private static final int COMPOUND_ELEMENT_BYTES = 36;

        @Override
        public void write(DataOutputStream output, CompoundTag compoundTag, TagVisitor<DataOutputStream> visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound;
            try {
                compound = visitor.visitAtCompound(output);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                compound = compoundTag.getValue().getDelegatedMap();
                for (String key : compound.keySet()) {
                    AbstractTag<?, ?, ?> tag = compoundTag.get(key);
                    if (tag.getType() == TagType.END) {
                        continue;
                    }
                    output.writeByte(tag.getType().getTypeId());

                    output.writeUTF(key);
                    tag.write(output, visitor);
                }
            }
            visitor.visitCompound(compound);

            output.writeByte(TagType.END.getTypeId());
        }

        @Override
        public CompoundTag read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound;
            int depth = limiter.incrementNestingDepth();
            try {
                compound = visitor.visitAtCompound(input);
            } catch (TagVisitor.FallbackDefaultCodecException e) {
                compound = readComponent(input, limiter, visitor);
            }
            limiter.setNestingDepthLevel(depth);

            visitor.visitCompound(compound);
            return new CompoundTag(compound)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, compound.keySet().stream().mapToInt(StringCodec::getBytesForLimiter).sum())
                    .incrementBytes(limiter, COMPOUND_BYTES)
                    .incrementBytes(limiter, COMPOUND_ELEMENT_BYTES * compound.size());
        }

        private Map<String, AbstractTag<?, ?, ?>> readComponent(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            Map<String, AbstractTag<?, ?, ?>> compound = new HashMap<>();
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
                AbstractTag<?, ?, ?> value = type.getCodec().read(input, limiter, visitor);
                if (value != null) {
                    compound.put(key, value);
                }
            }
            return compound;
        }
    }
}
