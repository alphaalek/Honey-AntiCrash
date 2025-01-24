package me.alek.honey.validation;

import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.validation.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum ValidationContainerRegistry {

    v1(FloodValidations::new),
    v2(MovementValidations::new),
    v3(HeldItemValidations::new),
    v4(CreativeValidations::new),
    v5(PayloadValidations::new),
    v6(NettyValidations::new),
    v7(NettyValidations.NettyB::new),
    v8(LoginValidations::new),
    v9(SignValidations::new),
    v10(BlockPlaceValidations::new),
    v11(ChatValidations::new),
    v12(WindowValidations::new),
    v13(RotationValidations::new),
    v14(BlockDigValidations::new),
    v15(TabValidations::new),
    v16(TabValidations.ClientboundTabValidations::new),
    v17(CommandValidations::new),
    v18(PickItemValidations::new),
    ;

    private static final List<Function<PacketLibrary, BaseValidationContainer>> validationFactories = new ArrayList<>();
    private static final List<BaseValidationContainer> validations = new ArrayList<>();

    static {
        for (ValidationContainerRegistry validation : ValidationContainerRegistry.values()) {
            validationFactories.add(validation.getValidationContainer());
        }
    }

    public static List<BaseValidationContainer> initValidations(PacketLibrary library) {
        validations.clear();
        validationFactories.stream().map(f -> f.apply(library)).forEach(validations::add);
        return validations;
    }

    public static List<BaseValidationContainer> getValidations() {
        return validations;
    }

    private final Function<PacketLibrary, BaseValidationContainer> validation;

    ValidationContainerRegistry(Function<PacketLibrary, BaseValidationContainer> validation) {
        this.validation = validation;
    }

    public Function<PacketLibrary, BaseValidationContainer> getValidationContainer() {
        return validation;
    }
}
