package me.alek.honey.netty.codec.types;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.CharsetUtil;
import me.alek.honey.netty.codec.PacketDataSerializer;

import java.nio.charset.StandardCharsets;

public class Utf8String implements Type<String, PacketDataSerializer> {

    private static final int MAX_BYTES_PER_CHAR_UTF8;

    static {
        MAX_BYTES_PER_CHAR_UTF8 = (int) CharsetUtil.getEncoder(CharsetUtil.UTF_8).maxBytesPerChar();
    }

    @Override
    public String read(PacketDataSerializer buf, Object... params) {
        if (params.length == 0) {
            throw new IllegalArgumentException("Null length");
        }
        int maxLength = Integer.parseInt(params[0].toString());

        int stringByteLength = buf.readVarInt();
        int maxByteLength = maxLength * MAX_BYTES_PER_CHAR_UTF8;
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

        if (decodedString.length() > maxLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + decodedString.length() + " > " + maxLength + ")");
        }

        return decodedString;
    }

    @Override
    public PacketDataSerializer write(PacketDataSerializer buf, String val, Object... params) {
        if (params.length == 0) {
            throw new IllegalArgumentException("Null length");
        }
        int maxLength = Integer.parseInt(params[0].toString());

        byte[] encodedByteArray = val.getBytes(StandardCharsets.UTF_8);
        if (encodedByteArray.length > maxLength) {
            throw new EncoderException("String too big (was " + encodedByteArray.length + " bytes encoded, max " + maxLength + ")");
        }

        Types.VAR_INT.write(buf, encodedByteArray.length);
        buf.writeBytes(encodedByteArray);

        return buf;
    }
}
