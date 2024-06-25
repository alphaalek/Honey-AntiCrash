package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

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
    v13(EncryptionValidations::new),
    v14(WindowValidations::new),
    v15(RotationValidations::new),
    v17(BlockDigValidations::new),
    v18(TabValidations::new),
    v19(CommandValidations::new),
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
