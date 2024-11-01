package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTAbstractArray;
import me.alek.honey.netty.nbt.array.NBTArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class CodecTypes {

    public static class EndCodec implements NBTCodec.Element<Void> {

        @Override
        public void write(DataOutput output, NBTElement<Void> tag) throws IOException {

        }

        @Override
        public NBTElement<Void> read(DataInput input) throws IOException {
            return NBTElement.END;
        }
    }

    public static class ByteCodec implements NBTCodec.Element<Byte> {

        @Override
        public void write(DataOutput output, NBTElement<Byte> tag) throws IOException {
            output.writeByte(tag.getValue());
        }

        @Override
        public NBTElement<Byte> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.BYTE, input.readByte());
        }
    }

    public static class ShortCodec implements NBTCodec.Element<Short> {

        @Override
        public void write(DataOutput output, NBTElement<Short> tag) throws IOException {
            output.writeShort(tag.getValue());
        }

        @Override
        public NBTElement<Short> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.SHORT, input.readShort());
        }
    }

    public static class IntCodec implements NBTCodec.Element<Integer> {

        @Override
        public void write(DataOutput output, NBTElement<Integer> tag) throws IOException {
            output.writeInt(tag.getValue());
        }

        @Override
        public NBTElement<Integer> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.INT, input.readInt());
        }
    }

    public static class LongCodec implements NBTCodec.Element<Long> {

        @Override
        public void write(DataOutput output, NBTElement<Long> tag) throws IOException {
            output.writeLong(tag.getValue());
        }

        @Override
        public NBTElement<Long> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.LONG, input.readLong());
        }
    }

    public static class FloatCodec implements NBTCodec.Element<Float> {

        @Override
        public void write(DataOutput output, NBTElement<Float> tag) throws IOException {
            output.writeFloat(tag.getValue());
        }

        @Override
        public NBTElement<Float> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.FLOAT, input.readFloat());
        }
    }

    public static class DoubleCodec implements NBTCodec.Element<Double> {

        @Override
        public void write(DataOutput output, NBTElement<Double> tag) throws IOException {
            output.writeDouble(tag.getValue());
        }

        @Override
        public NBTElement<Double> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.DOUBLE, input.readDouble());
        }
    }

    public static class ByteArrayCodec implements NBTCodec.Element<byte[]> {

        @Override
        public void write(DataOutput output, NBTElement<byte[]> tag) throws IOException {
            if (tag.getValue() == null) {
                output.writeInt(0);
            } else {
                output.writeInt(tag.getValue().length);
                output.write(tag.getValue());
            }
        }

        @Override
        public NBTElement<byte[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readByte();
            }

            return new NBTElement<>(NBTType.ARRAY_OF_BYTE, array);
        }
    }

    public static class StringCodec implements NBTCodec.Element<String> {

        @Override
        public void write(DataOutput output, NBTElement<String> tag) throws IOException {
            output.writeUTF(tag.getValue());
        }

        @Override
        public NBTElement<String> read(DataInput input) throws IOException {
            return new NBTElement<>(NBTType.STRING, input.readUTF());
        }
    }

    public static class LongArrayCodec implements NBTCodec.Element<long[]> {

        @Override
        public void write(DataOutput output, NBTElement<long[]> tag) throws IOException {
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
        public NBTElement<long[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readLong();
            }

            return new NBTElement<>(NBTType.ARRAY_OF_LONG, array);
        }
    }

    public static class IntArrayCodec implements NBTCodec.Element<int[]> {

        @Override
        public void write(DataOutput output, NBTElement<int[]> tag) throws IOException {
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
        public NBTElement<int[]> read(DataInput input) throws IOException {
            int length = input.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readInt();
            }

            return new NBTElement<>(NBTType.ARRAY_OF_INT, array);
        }
    }

    public static class ArrayCodec implements NBTCodec<NBTAbstractArray<? extends NBTBase<?, ?>>> {

        @Override
        public void write(DataOutput output, NBTAbstractArray<? extends NBTBase<?, ?>> tag) throws IOException {
            if (tag.getValue().isEmpty()) {
                output.writeByte(0);
            } else {
                output.writeByte(tag.getType().getTypeId());
            }
            output.writeInt(tag.getValue().size());
            for (NBTBase<?, ?> tagInList : tag) {
                tagInList.write(output);
            }
        }

        @Override
        public NBTAbstractArray<? extends NBTBase<?, ?>> read(DataInput input) throws IOException {
            NBTType type = NBTType.getFromTypeId(input.readByte());
            if (type == null) {
                throw new IOException();
            }
            int length = input.readInt();
            List<Object> array = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                array.set(i, Optional.ofNullable(type.getCodec().read(input)).map(NBTBase::getValue).orElse(null));
            }

            switch (type) {
                case ARRAY:
                    return new GeneticArrayTypes.NBTArrayOfArray(array);
                case COMPOUND:
                    return new GeneticArrayTypes.NBTArrayOfCompound(array);
                default:
                    return new NBTArray<>(array, type);
            }
        }
    }

    public static class CompoundCodec implements NBTCodec<NBTCompound> {

        @Override
        public void write(DataOutput output, NBTCompound tag) throws IOException {
            for (String key : tag.getValue().keySet()) {
                NBTBase<?, ?> tagInCompound = tag.getValue().getConverted(key);
                output.writeByte((byte) tagInCompound.getType().getTypeId());
                if (tagInCompound.getType() == NBTType.END) {
                    continue;
                }

                output.writeUTF(key);
                tagInCompound.write(output);
            }

            output.writeByte(0);
        }

        @Override
        public NBTCompound read(DataInput input) throws IOException {
            Map<String, Object> map = new HashMap<>();

            byte b;
            while ((b = input.readByte()) != 0) {
                String key = input.readUTF();
                NBTType type = NBTType.getFromTypeId(b);
                if (type == null) {
                    throw new IOException();
                }

                Object value = Optional.ofNullable(type.getCodec().read(input)).map(NBTBase::getValue).orElse(null);

                map.put(key, value);
            }

            return new NBTCompound(map);
        }
    }
}
