package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public enum ListenerRegistry {

    v1(FloodListener::new),
    v2(PositionListener::new),
    v3(HeldItemListener::new),
    v4(CreativeSlotListener::new),
    v5(CustomPayloadListener::new),
    v6(NettyListener::new),
    v7(NettyListener.NettyB::new),
    v8(NettyDecodeListener::new),
    v9(LoginStartListener::new),
    v10(UpdateSignListener::new),
    v11(BlockPlaceListener::new),
    v12(ChatListener::new),
    v13(EncryptionBeginListener::new),
    v14(WindowClickListener::new),
    v15(RotationListener::new),
    v17(BlockDigListener::new),
    v18(TabCompleteListener::new);


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
