package me.alek.honey.netty.codec.types;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.CharsetUtil;
import me.alek.honey.netty.codec.PacketDataSerializer;

import java.nio.charset.StandardCharsets;

public class Utf8String implements TypeSerializer<String, TypeSerializer.TypeContext.OneArg<Integer>> {

    private static final int MAX_BYTES_PER_CHAR_UTF8;

    static {
        MAX_BYTES_PER_CHAR_UTF8 = (int) CharsetUtil.getEncoder(CharsetUtil.UTF_8).maxBytesPerChar();
    }

    @Override
    public String read(PacketDataSerializer buf, TypeSerializer.TypeContext.OneArg<Integer> context) {
        int stringByteLength = buf.readVarInt();
        int maxByteLength = context.get() * MAX_BYTES_PER_CHAR_UTF8;
        if (stringByteLength > maxByteLength) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + stringByteLength + " > " + maxByteLength + ")");
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

        if (decodedString.length() > context.get()) {
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

        Types.VAR_INT.write(buf, encodedByteArray.length, null);
        buf.writeBytes(encodedByteArray);

        return buf;
    }
}
