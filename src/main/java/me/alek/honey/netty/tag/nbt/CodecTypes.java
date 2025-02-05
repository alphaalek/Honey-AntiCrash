package me.alek.honey.netty.tag.nbt;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;
import sun.nio.cs.KOI8_U;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Primitive<Void> {

        private static final int END_BYTES = 8;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Void> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            visitor.visitEnd();
            visitor.visitAtEnd(output);
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
            byte val = tag.getValue();
            visitor.visitByte(val);
            output.writeByte(val);
            visitor.visitAtByte(output);
        }

        @Override
        public PrimitiveTag<Byte> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtByte(input);
            byte val = input.readByte();
            visitor.visitByte(val);
            return new PrimitiveTag<>(TagType.BYTE, val)
                    .incrementBytes(limiter, BYTE_BYTES);
        }
    }

    public static class ShortCodec implements NBTCodec.Primitive<Short> {

        private static final int SHORT_BYTES = 10;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Short> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            short val = tag.getValue();
            visitor.visitShort(val);
            output.writeShort(val);
            visitor.visitAtShort(output);
        }

        @Override
        public PrimitiveTag<Short> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtShort(input);
            short val = input.readShort();
            visitor.visitShort(val);
            return new PrimitiveTag<>(TagType.SHORT, val)
                    .incrementBytes(limiter, SHORT_BYTES);
        }
    }

    public static class IntCodec implements NBTCodec.Primitive<Integer> {

        private static final int INT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Integer> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            int val = tag.getValue();
            visitor.visitInt(val);
            output.writeInt(val);
            visitor.visitAtInt(output);
        }

        @Override
        public PrimitiveTag<Integer> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtInt(input);
            int val = input.readInt();
            visitor.visitInt(val);
            return new PrimitiveTag<>(TagType.INT, val)
                    .incrementBytes(limiter, INT_BYTES);
        }
    }

    public static class LongCodec implements NBTCodec.Primitive<Long> {

        private static final int LONG_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Long> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            long val = tag.getValue();
            visitor.visitLong(val);
            output.writeLong(val);
            visitor.visitAtLong(output);
        }

        @Override
        public PrimitiveTag<Long> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtLong(input);
            long val = input.readLong();
            visitor.visitLong(val);
            return new PrimitiveTag<>(TagType.LONG, val)
                    .incrementBytes(limiter, LONG_BYTES);
        }
    }

    public static class FloatCodec implements NBTCodec.Primitive<Float> {

        private static final int FLOAT_BYTES = 12;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Float> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            float val = tag.getValue();
            visitor.visitFloat(val);
            output.writeFloat(val);
            visitor.visitAtFloat(output);
        }

        @Override
        public PrimitiveTag<Float> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtFloat(input);
            float val = input.readFloat();
            visitor.visitFloat(val);
            return new PrimitiveTag<>(TagType.FLOAT, val)
                    .incrementBytes(limiter, FLOAT_BYTES);
        }
    }

    public static class DoubleCodec implements NBTCodec.Primitive<Double> {

        private static final int DOUBLE_BYTES = 16;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<Double> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            double val = tag.getValue();
            visitor.visitDouble(val);
            output.writeDouble(tag.getValue());
            visitor.visitAtDouble(output);
        }

        @Override
        public PrimitiveTag<Double> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtDouble(input);
            double val = input.readDouble();
            visitor.visitDouble(val);
            return new PrimitiveTag<>(TagType.DOUBLE, val)
                    .incrementBytes(limiter, DOUBLE_BYTES);
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Primitive<byte[]> {

        private static final int BYTE_ARRAY_BYTES = 24;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<byte[]> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            byte[] array = tag.getValue();
            visitor.visitByteArray(array);
            output.writeInt(array.length);
            output.write(array);
            visitor.visitAtByteArray(output);
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtByteArray(input);
            int length = input.readInt();
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readByte();
            }
            visitor.visitByteArray(array);

            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, array)
                    .incrementBytes(limiter, BYTE_ARRAY_BYTES)
                    .incrementBytes(limiter, length);
        }
    }

    public static class StringCodec implements NBTCodec.Primitive<String> {

        private static final int STRING_BYTES = 36;
        private static final int CHAR_BYTES = 2;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<String> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            String str = tag.getValue();
            visitor.visitString(str);
            output.writeUTF(str);
            visitor.visitAtString(output);
        }

        @Override
        public PrimitiveTag<String> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtString(input);
            String str = input.readUTF();
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
            long[] array = tag.getValue();
            visitor.visitLongArray(array);
            output.writeInt(array.length);
            for (long value : array) {
                output.writeLong(value);
            }
            visitor.visitAtLongArray(output);
        }

        @Override
        public PrimitiveTag<long[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtLongArray(input);
            int length = input.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readLong();
            }

            visitor.visitLongArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, array)
                    .incrementBytes(limiter, LONG_ARRAY_BYTES)
                    .incrementBytes(limiter, length * LONG_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Primitive<int[]> {

        private static final int INT_ARRAY_BYTES = 24;
        private static final int INT_ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, PrimitiveTag<int[]> tag, TagVisitor<DataOutputStream> visitor) throws IOException {
            int[] array = tag.getValue();
            visitor.visitIntArray(array);
            output.writeInt(array.length);
            for (int value : array) {
                output.writeInt(value);
            }
            visitor.visitAtIntArray(output);
        }

        @Override
        public PrimitiveTag<int[]> read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtIntArray(input);
            int length = input.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readInt();
            }

            visitor.visitIntArray(array);
            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, array)
                    .incrementBytes(limiter, INT_ARRAY_BYTES)
                    .incrementBytes(limiter, length * INT_ARRAY_ELEMENT_BYTES);
        }
    }

    public static class ArrayCodec<T extends AbstractTag<?, ?, T>, R extends AbstractArrayTag<T, R, ?>> implements NBTCodec<R> {

        private static final int ARRAY_BYTES = 37;
        private static final int ARRAY_ELEMENT_BYTES = 4;

        @Override
        public void write(DataOutputStream output, R array, TagVisitor<DataOutputStream> visitor) throws IOException {
            visitor.visitArray(array.getValues());
            if (array.getValue().isEmpty()) {
                output.writeByte(TagType.END.getTypeId());
            } else {
                output.writeByte(Optional.ofNullable(array.getTypeOf()).map(TagType::getTypeId).orElseThrow(IllegalStateException::new));
            }
            output.writeInt(array.getValue().size());
            for (AbstractTag<?, ?, ?> tag : array.getValue()) {
                tag.write(output, visitor);
            }
            visitor.visitAtArray(output);
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public R read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtArray(input);
            TagType type = TagType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException("Invalid type");
            }
            int depth = limiter.incrementNestingDepth();

            int length = input.readInt();
            List<T> array = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                array.add(type.<T>getCodec().read(input, limiter, visitor));
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

        private <ARRAY extends GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>, IN extends AbstractArrayTag<C, IN, ?>, C extends AbstractTag<?, ?, C>> GeneticArrayTypes.ArrayOfArrayTag<?, IN, C> createArrayOfArray(Iterable<T> array) {
            return new GeneticArrayTypes.ArrayOfArrayTag<ARRAY, IN, C>(array);
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        private static final int COMPOUND_BYTES = 48;
        private static final int COMPOUND_ELEMENT_BYTES = 36;

        @Override
        public void write(DataOutputStream output, CompoundTag compound, TagVisitor<DataOutputStream> visitor) throws IOException {
            visitor.visitCompound(compound.getValue().getDelegatedMap());
            for (String key : compound.getValue().keySet()) {
                AbstractTag<?, ?, ?> tag = compound.getValue().getConverted(key);
                if (tag.getType() == TagType.END) {
                    continue;
                }
                output.writeByte(tag.getType().getTypeId());

                output.writeUTF(key);
                tag.write(output, visitor);
            }

            output.writeByte(TagType.END.getTypeId());
            visitor.visitAtCompound(output);
        }

        @Override
        public CompoundTag read(DataInputStream input, TagLimiter limiter, TagVisitor<DataInputStream> visitor) throws IOException {
            visitor.visitAtCompound(input);
            Map<String, AbstractTag<?, ?, ?>> compound = new HashMap<>();

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
                AbstractTag<?, ?, ?> value = type.getCodec().read(input, limiter, visitor);
                if (value != null) {
                    compound.put(key, value);
                }
            }
            limiter.setNestingDepthLevel(depth);

            visitor.visitCompound(compound);
            return new CompoundTag(compound)
                    .incrementComplexity(limiter)
                    .incrementBytes(limiter, compound.keySet().stream().mapToInt(StringCodec::getBytesForLimiter).sum())
                    .incrementBytes(limiter, COMPOUND_BYTES)
                    .incrementBytes(limiter, COMPOUND_ELEMENT_BYTES * compound.size());
        }
    }
}
