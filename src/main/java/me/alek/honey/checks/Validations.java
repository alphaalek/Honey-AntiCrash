package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum Validations {

    v1(FloodValidation::new),
    v2(MovementValidation::new),
    v3(HeldItemValidation::new),
    v4(CreativeValidation::new),
    v5(PayloadValidation::new),
    v6(NettyValidation::new),
    v7(NettyValidation.NettyB::new),
    v8(NettyDecodeValidation::new),
    v9(LoginValidation::new),
    v10(SignValidation::new),
    v11(BlockPlaceValidation::new),
    v12(ChatValidation::new),
    v13(EncryptionValidation::new),
    v14(WindowValidation::new),
    v15(RotationValidation::new),
    v17(BlockDigValidation::new),
    v18(TabValidation::new),
    v19(CommandValidation::new),
    ;

    private static List<IListener> validations;

    public static synchronized List<IListener> getValidations() {
        if (validations == null) {
            List<IListener> checkList = new ArrayList<>();
            for (Validations check : Validations.values()) {
                checkList.add(check.getCheck().get());
            }
            validations = checkList;
        }
        return validations;
    }

    private final Supplier<IListener> validation;

    Validations(Supplier<IListener> check) {
        this.validation = check;
    }

    public Supplier<IListener> getCheck() {
        return validation;
    }
}
