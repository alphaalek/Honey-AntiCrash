package me.alek.honey.validation;

import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.validation.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ValidationContainers {

    v1(FloodValidations::new),
    v2(MovementValidations::new),
    v3(HeldItemValidations::new),
    v4(CreativeValidations::new),
    v5(PayloadValidations::new),
    v6(NettyValidations::new),
    v7(NettyValidations.NettyB::new),
    v8(NettyDecodeValidations::new),
    v9(LoginValidations::new),
    v10(SignValidations::new),
    v11(BlockPlaceValidations::new),
    v12(ChatValidations::new),
    v13(WindowValidations::new),
    v14(RotationValidations::new),
    v15(BlockDigValidations::new),
    v16(TabValidations::new),
    v17(TabValidations.ClientboundTabValidations::new),
    v18(CommandValidations::new),
    ;

    private static final List<Function<PacketLibrary, BaseValidationContainer>> validationFactories = new ArrayList<>();
    private static final List<BaseValidationContainer> validations = new ArrayList<>();

    static {
        for (ValidationContainers validation : ValidationContainers.values()) {
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

    ValidationContainers(Function<PacketLibrary, BaseValidationContainer> validation) {
        this.validation = validation;
    }

    public Function<PacketLibrary, BaseValidationContainer> getValidationContainer() {
        return validation;
    }
}
