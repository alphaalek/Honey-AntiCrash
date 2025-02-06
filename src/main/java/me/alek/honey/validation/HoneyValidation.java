package me.alek.honey.validation;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.util.id.IntIdentifiable;

public interface HoneyValidation<T> extends IntIdentifiable {

    static <T> HoneyValidation<T> of(String name, String description, int id) {
        return new HoneyValidation<T>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public ValidationViolationContext validate(HoneyPlayer player, T type, ValidationPipelineContext context) {
                return ValidationViolationContext.PASS;
            }

            @Override
            public Integer getID() {
                return id;
            }
        };
    }

    default ViolationKey generateKey() {
        return ViolationKey.of(this);
    }

    String getName();

    String getDescription();

    ValidationViolationContext validate(HoneyPlayer player, T type, ValidationPipelineContext context);

    default boolean isViolation() {
        return false;
    }
}
