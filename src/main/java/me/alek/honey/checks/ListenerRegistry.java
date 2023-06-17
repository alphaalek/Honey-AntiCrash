package me.alek.honey.checks;

import me.alek.honey.checks.listeners.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum ListenerRegistry {

    v1(Flood::new),
    v2(Position::new),
    v3(HeldItem::new),
    v4(CreativeSlot::new),
    v5(CustomPayload::new),
    v6(NumerAttribute::new),
    v7(LoginStart::new),
    v8(UpdateSign::new),
    v9(BlockPlace::new),
    v10(Chat::new),
    v11(EncryptionBegin::new),
    v12(TabComplete::new),
    v13(WindowClick::new);

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

    ListenerRegistry(Supplier<IListener> check) {
        this.check = check;
    }

    public Supplier<IListener> getCheck() {
        return check;
    }
}
