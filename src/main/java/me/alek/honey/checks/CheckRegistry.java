package me.alek.honey.checks;

import me.alek.honey.checks.impl.chat.Commands;
import me.alek.honey.checks.impl.chat.IllegalChatMessage;
import me.alek.honey.checks.impl.chat.TabComplete;
import me.alek.honey.checks.impl.velocity.IllegalVelocity;
import me.alek.honey.checks.impl.blocks.IllegalSign;
import me.alek.honey.checks.impl.online.AlreadyOnline;
import me.alek.honey.checks.impl.general.IllegalAttribute;
import me.alek.honey.checks.impl.online.NotOnline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum CheckRegistry {

    // velocity
    ILLEGAL_VELOCITY(IllegalVelocity::new),

    // general
    ILLEGAL_ATTRIBUTE(IllegalAttribute::new),

    // online
    ALREADY_ONLINE(AlreadyOnline::new),
    NOT_ONLINE(NotOnline::new),

    // block
    ILLEGAL_SIGN(IllegalSign::new),

    // chat
    COMMANDS_(Commands::new),
    ILLEGAL_CHAT(IllegalChatMessage::new),
    TAB_COMPLETE(TabComplete::new);

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
