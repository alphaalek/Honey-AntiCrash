package me.alek.honey.validation;

import me.alek.honey.HoneyKey;
import me.alek.honey.validation.types.HoneyValidation;

import java.util.Objects;

public class ViolationKey extends HoneyKey {

    public static ViolationKey of(HoneyValidation<?> validation) {
        return new ViolationKey(validation, HoneyKey.of(validation.getID(), validation.getName()));
    }

    public static ViolationKey of(HoneyValidation<?> validation, HoneyKey key) {
        return new ViolationKey(validation, key);
    }

    private final HoneyValidation<?> validation;
    private final HoneyKey key;

    public ViolationKey(HoneyValidation<?> validation, HoneyKey key) {
        super(key.getRegistryId(), key.getIdentifier());
        this.validation = validation;
        this.key = key;
    }

    public HoneyValidation<?> getValidation() {
        return validation;
    }

    public HoneyKey getKey() {
        return key;
    }

    @Override
    public boolean equals(Object to) {
        if (!(to instanceof ViolationKey)) {
            return false;
        }

        ViolationKey key = (ViolationKey) to;
        return Objects.equals(key.key, this.key)
                && key.validation.equals(this.validation);
    }
}
