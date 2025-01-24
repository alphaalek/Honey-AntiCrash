package me.alek.honey.netty.codec.validation;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class CodecType<T, C extends DecodeContext<?>> {

    public static final CodecType<WrappedImmutableItemStack, DecodeContext<?>> ITEM_STACK = new CodecType<>();
    public static final CodecType<WrappedImmutableItemStack, DecodeContext.BookEditContext<?>> BOOK_EDIT_ITEM_STACK = new CodecType<>();
    public static final CodecType<CompoundTag, DecodeContext.BookEditContext<?>> BOOK_EDIT_TAG = new CodecType<>();

    private final List<PostDecodeValidation<T, C>> validations = new ArrayList<>();

    public void validate(T value, C context) throws DecoderException {
        for (PostDecodeValidation<T, C> validation : this.validations) {
            validation.validate(value, context);
        }
    }

    public void addValidation(PostDecodeValidation<T, C> validation) {
        this.validations.add(validation);
    }

    public boolean removeValidation(PostDecodeValidation<T, C> validation) {
        return this.validations.remove(validation);
    }

    public void clearValidations() {
        this.validations.clear();
    }

    public List<PostDecodeValidation<T, C>> getValidations() {
        return this.validations;
    }
}
