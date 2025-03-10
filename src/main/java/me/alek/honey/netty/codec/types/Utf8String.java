package me.alek.honey.netty.codec.types;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.TypeSerializer;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Utf8String implements TypeSerializer<String, TypeSerializer.TypeContext.OneArg<Integer>> {

    @Override
    public String read(PacketDataSerializer buf, TypeSerializer.TypeContext.OneArg<Integer> context) {
        int stringByteLength = buf.readVarInt();
        int maxByteLength = context.get();
        if (maxByteLength != -1 && stringByteLength > maxByteLength) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + stringByteLength + " > " + context.get() + ")");
        }

        if (stringByteLength < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }

        int remaining = buf.readableBytes();
        if (stringByteLength > remaining) {
            throw new DecoderException("Not enough bytes in buffer, expected " + stringByteLength + ", but got " + remaining);
        }

        String decodedString = buf.toString(buf.readerIndex(), stringByteLength, StandardCharsets.UTF_8);
        buf.skipBytes(stringByteLength);

        if (decodedString.length() > maxByteLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + decodedString.length() + " > " + context.get() + ")");
        }

        return decodedString;
    }

    @Override
    public PacketDataSerializer write(PacketDataSerializer buf, String val, TypeContext.OneArg<Integer> context) {
        byte[] encodedByteArray = val.getBytes(StandardCharsets.UTF_8);
        if (encodedByteArray.length > context.get()) {
            throw new EncoderException("String too big (was " + encodedByteArray.length + " bytes encoded, max " + context.get() + ")");
        }

        buf.writeVarInt(encodedByteArray.length);
        buf.writeBytes(encodedByteArray);

        return buf;
    }

    public static String readUTFInputStream(DataInputStream stream, int maxByteLength) throws IOException {
        int length = stream.readUnsignedShort();
        if (length > maxByteLength) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + length + " > " + maxByteLength + ")");
        }

        byte[] buf = new byte[length];
        stream.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }
}
