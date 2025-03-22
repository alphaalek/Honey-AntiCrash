package me.alek.honey.netty.codec;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;

public class StackDecodingException extends DecoderException {

    private final WrappedImmutableItemStack stack;

    public StackDecodingException(Throwable cause, WrappedImmutableItemStack stack) {
        super(cause);
        this.stack = stack;
    }

    public WrappedImmutableItemStack getStack() {
        return this.stack;
    }
}
