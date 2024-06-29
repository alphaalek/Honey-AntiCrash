package me.alek.honey.validation;

import me.alek.honey.services.ViolationService;
import me.alek.honey.validation.types.HoneyValidation;

import java.util.Objects;

public class ViolationKey {

    public static ViolationKey of(HoneyValidation<?> validation) {
        return new ViolationKey(validation, null);
    }

    public static ViolationKey of (HoneyValidation<?> validation, ViolationService.HoneyKey key) {
        return new ViolationKey(validation, key);
    }

    private final HoneyValidation<?> validation;
    private final ViolationService.HoneyKey key;

    public ViolationKey(HoneyValidation<?> validation, ViolationService.HoneyKey key) {
        this.validation = validation;
        this.key = key;
    }

    public HoneyValidation<?> getValidation() {
        return validation;
    }

    public ViolationService.HoneyKey getKey() {
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
