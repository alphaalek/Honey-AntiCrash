package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public enum ListenerRegistry {

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
    v18(TabValidation::new);


    private final Supplier<IListener> check;
    private static List<IListener> checks;

    public static synchronized List<IListener> getChecks() {
        if (checks == null) {
            List<IListener> checkList = new ArrayList<>();
            for (ListenerRegistry check : ListenerRegistry.values()) {
                checkList.add(check.getCheck().get());
            }
            checks = checkList;
        }
        return checks;
    }

    public static int getMaxID() {
        AtomicInteger maxID = new AtomicInteger();
        for (IListener listener : getChecks()) {
            if (listener instanceof CheckVisitable) {
                ((CheckVisitable)listener).getVisitor(null)
                        .getChecks().stream()
                                .filter(check -> check.getID() > maxID.get())
                                .forEach(check -> maxID.set(check.getID()));
            }
        }
        return maxID.get();
    }

    ListenerRegistry(Supplier<IListener> check) {
        this.check = check;
    }

    public Supplier<IListener> getCheck() {
        return check;
    }
}
