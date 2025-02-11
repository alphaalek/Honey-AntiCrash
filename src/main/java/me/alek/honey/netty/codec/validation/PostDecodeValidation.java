package me.alek.honey.netty.codec.validation;

import io.netty.handler.codec.DecoderException;

public interface PostDecodeValidation<T, C extends CodecContext> {

    void validate(T value, C context) throws DecoderException;
}
