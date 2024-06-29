package me.alek.honey.validation;

import me.alek.honey.validation.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum Validations {


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

    v17(CommandValidations::new),
    ;

    private static List<BaseValidation> validations;

    public static synchronized List<BaseValidation> getValidations() {
        if (validations == null) {
            List<BaseValidation> checkList = new ArrayList<>();
            for (Validations check : Validations.values()) {
                checkList.add(check.getCheck().get());
            }
            validations = checkList;
        }
        return validations;
    }

    private final Supplier<BaseValidation> validation;

    Validations(Supplier<BaseValidation> check) {
        this.validation = check;
    }

    public Supplier<BaseValidation> getCheck() {
        return validation;
    }
}
