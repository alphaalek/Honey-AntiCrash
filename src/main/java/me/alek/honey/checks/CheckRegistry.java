package me.alek.honey.checks;

import me.alek.honey.checks.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum CheckRegistry {

    FLOOD(Flood::new),
    ILLEGAL_VELOCITY(IllegalVelocity::new),
    HELD_ITEM(HeldItem::new),
    CREATIVE_SLOT(CreativeSlot::new),
    CUSTOM_PAYLOAD(CustomPayload::new),
    ILLEGAL_ATTRIBUTE(IllegalAttribute::new),
    ALREADY_ONLINE(AlreadyOnline::new),
    NOT_ONLINE(NotOnline::new),
    ILLEGAL_SIGN(IllegalSign::new),
    BLOCK_PLACEMENT(BlockPlacement::new),
    COMMANDS_(Commands::new),
    ILLEGAL_CHAT(IllegalChatMessage::new),
    TAB_COMPLETE(TabComplete::new),
    WINDOW_CLICK(WindowClick::new);

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
