package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.nbt.array.ArrayTag;
import org.bukkit.Bukkit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Element<Void> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Void> tag) throws IOException {

        }

        @Override
        public PrimitiveTag<Void> read(DataInput input) throws IOException {
            return PrimitiveTag.END;
        }
    }

    public static class ByteCodec implements NBTCodec.Element<Byte> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Byte> tag) throws IOException {
            output.writeByte(tag.getValue());
        }

        @Override
        public PrimitiveTag<Byte> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.BYTE, input.readByte());
        }
    }

    public static class ShortCodec implements NBTCodec.Element<Short> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Short> tag) throws IOException {
            output.writeShort(tag.getValue());
        }

        @Override
        public PrimitiveTag<Short> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.SHORT, input.readShort());
        }
    }

    public static class IntCodec implements NBTCodec.Element<Integer> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Integer> tag) throws IOException {
            output.writeInt(tag.getValue());
        }

        @Override
        public PrimitiveTag<Integer> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.INT, input.readInt());
        }
    }

    public static class LongCodec implements NBTCodec.Element<Long> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Long> tag) throws IOException {
            output.writeLong(tag.getValue());
        }

        @Override
        public PrimitiveTag<Long> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.LONG, input.readLong());
        }
    }

    public static class FloatCodec implements NBTCodec.Element<Float> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Float> tag) throws IOException {
            output.writeFloat(tag.getValue());
        }

        @Override
        public PrimitiveTag<Float> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.FLOAT, input.readFloat());
        }
    }

    public static class DoubleCodec implements NBTCodec.Element<Double> {

        @Override
        public void write(DataOutput output, PrimitiveTag<Double> tag) throws IOException {
            output.writeDouble(tag.getValue());
        }

        @Override
        public PrimitiveTag<Double> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.DOUBLE, input.readDouble());
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Element<byte[]> {

        @Override
        public void write(DataOutput output, PrimitiveTag<byte[]> tag) throws IOException {
            if (tag.getValue() == null) {
                output.writeInt(0);
            } else {
                output.writeInt(tag.getValue().length);
                output.write(tag.getValue());
            }
        }

        @Override
        public PrimitiveTag<byte[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readByte();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, array);
        }
    }

    public static class StringCodec implements NBTCodec.Element<String> {

        @Override
        public void write(DataOutput output, PrimitiveTag<String> tag) throws IOException {
            output.writeUTF(tag.getValue());
        }

        @Override
        public PrimitiveTag<String> read(DataInput input) throws IOException {
            return new PrimitiveTag<>(TagType.STRING, input.readUTF());
        }
    }

    public static class LongArrayCodec implements NBTCodec.Element<long[]> {

        @Override
        public void write(DataOutput output, PrimitiveTag<long[]> tag) throws IOException {
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
        public PrimitiveTag<long[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readLong();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, array);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Element<int[]> {

        @Override
        public void write(DataOutput output, PrimitiveTag<int[]> tag) throws IOException {
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
        public PrimitiveTag<int[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readInt();
            }

            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, array);
        }
    }

    public static class ArrayCodec implements NBTCodec<AbstractArrayTag<? extends AbstractTag<?, ?>>> {

        @Override
        public void write(DataOutput output, AbstractArrayTag<? extends AbstractTag<?, ?>> tag) throws IOException {
            if (tag.getValue().isEmpty()) {
                output.writeByte(0);
            } else {
                output.writeByte(Optional.ofNullable(tag.getTypeOf()).map(TagType::getTypeId).orElse((byte) 0));
            }
            output.writeInt(tag.getValue().size());
            for (AbstractTag<?, ?> tagInList : tag) {
                tagInList.write(output);
            }
        }

        @Override
        public AbstractArrayTag<? extends AbstractTag<?, ?>> read(DataInput input) throws IOException {
            TagType type = TagType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException();
            }
            int length = input.readInt();
            List<Object> array = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                array.add(Optional.ofNullable(type.getCodec().read(input)).map(AbstractTag::getHandle).orElse(null));
            }

            if (type.hasCollection()) {
                switch (type.getCollection()) {
                    case ARRAY:
                        return new GeneticArrayTypes.ArrayOfArrayTag(array);
                    case COMPOUND:
                        return new GeneticArrayTypes.ArrayOfCompoundTag(array);
                }
            }

            return new ArrayTag<>(array, type);
        }
    }

    public static class CompoundCodec implements NBTCodec<CompoundTag> {

        @Override
        public void write(DataOutput output, CompoundTag tag) throws IOException {
            for (String key : tag.getValue().keySet()) {
                AbstractTag<?, ?> tagInCompound = tag.getValue().getConverted(key);
                output.writeByte(tagInCompound.getType().getTypeId());
                if (tagInCompound.getType() == TagType.END) {
                    continue;
                }

                output.writeUTF(key);
                tagInCompound.write(output);
            }

            output.writeByte(0);
        }

        @Override
        public CompoundTag read(DataInput input) throws IOException {
            Map<String, Object> map = new HashMap<>();

            byte b;
            while ((b = input.readByte()) != 0) {
                TagType type = TagType.getFromTypeId(b);
                if (type == null) {
                    throw new IOException();
                }

                String key = input.readUTF();
                Object value = Optional.ofNullable(type.getCodec().read(input)).map(AbstractTag::getHandle).orElse(null);

                map.put(key, value);
            }

            return new CompoundTag(map);
        }
    }
}
