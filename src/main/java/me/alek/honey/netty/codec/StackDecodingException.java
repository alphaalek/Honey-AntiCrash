package me.alek.honey.netty.codec;

import io.netty.handler.codec.DecoderException;

public class StackDecodingException extends DecoderException {

    public StackDecodingException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public StackDecodingException(Throwable cause) {
        super(cause);
    }

    public StackDecodingException(String msg) {
        super(msg);
    }
}
