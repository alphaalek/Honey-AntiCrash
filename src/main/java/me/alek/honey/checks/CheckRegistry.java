package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum CheckRegistry {

    v1(Flood::new),
    v2(IllegalVelocity::new),
    v3(HeldItem::new),
    v4(CreativeSlot::new),
    v5(CustomPayload::new),
    v6(IllegalAttribute::new),
    v7(AlreadyOnline::new),
    v9(NotOnline::new),
    v10(IllegalSign::new),
    v11(BlockPlacement::new),
    v12(Commands::new),
    v13(IllegalChatMessage::new),
    v14(NullPing.NullPingLogin::new),
    v15(NullPing.NullPingEncryption::new),
    v16(TabComplete::new),
    v17(WindowClick::new);

    private final Supplier<Check> check;
    private static List<Check> checks;

    public static synchronized List<Check> getChecks() {
        if (checks == null) {
            List<Check> checkList = new ArrayList<>();
            for (CheckRegistry check : CheckRegistry.values()) {
                checkList.add(check.getCheck().get());
            }
            checks = checkList;
        }
        return checks;
    }

    CheckRegistry(Supplier<Check> check) {
        this.check = check;
    }

    public Supplier<Check> getCheck() {
        return check;
    }
}
