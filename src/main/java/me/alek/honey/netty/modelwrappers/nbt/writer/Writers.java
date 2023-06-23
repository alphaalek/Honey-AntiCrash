package me.alek.honey.netty.modelwrappers.nbt.writer;

import me.alek.honey.netty.modelwrappers.nbt.NBTElement;

import java.io.DataOutput;

public class Writers {

    public static class ByteWriter implements NBTWriter<Byte> {

        @Override
        public void write(DataOutput output, NBTElement<Byte> element) throws Exception {
            output.writeByte(element.getValue());
        }
    }

    public static class ShortWriter implements NBTWriter<Short> {

        @Override
        public void write(DataOutput output, NBTElement<Short> element) throws Exception {
            output.writeShort(element.getValue());
        }
    }

    public static class IntWriter implements NBTWriter<Integer> {

        @Override
        public void write(DataOutput output, NBTElement<Integer> element) throws Exception {
            output.writeInt(element.getValue());
        }
    }

    public static class LongWriter implements NBTWriter<Long> {

        @Override
        public void write(DataOutput output, NBTElement<Long> element) throws Exception {
            output.writeLong(element.getValue());
        }
    }

    public static class FloatWriter implements NBTWriter<Float> {

        @Override
        public void write(DataOutput output, NBTElement<Float> element) throws Exception {
            output.writeFloat(element.getValue());
        }
    }

    public static class DoubleWriter implements NBTWriter<Double> {

        @Override
        public void write(DataOutput output, NBTElement<Double> element) throws Exception {
            output.writeDouble(element.getValue());
        }
    }

    public static class ByteArrayWriter implements NBTWriter<byte[]> {

        @Override
        public void write(DataOutput output, NBTElement<byte[]> element) throws Exception {
            output.writeInt(element.getValue().length);
            output.write(element.getValue());
        }
    }

    public static class StringWriter implements NBTWriter<String> {

        @Override
        public void write(DataOutput output, NBTElement<String> element) throws Exception {
            output.writeUTF(element.getValue());
        }
    }

    public static class LongArrayWriter implements NBTWriter<long[]> {

        @Override
        public void write(DataOutput output, NBTElement<long[]> element) throws Exception {
            output.writeInt(element.getValue().length);
            for (long value : element.getValue()) {
                output.writeLong(value);
            }
        }
    }

    public static class IntArrayWriter implements NBTWriter<int[]> {

        @Override
        public void write(DataOutput output, NBTElement<int[]> element) throws Exception {
            output.writeInt(element.getValue().length);
            for (int value : element.getValue()) {
                output.writeInt(value);
            }
        }
    }
}
