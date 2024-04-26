package me.alek.honey.monitor.exceptions;

import me.alek.honey.monitor.MonitorFlag;
import me.alek.honey.monitor.MonitorFlagParser;

import java.util.Optional;

public class WrongMonitorFlagSyntaxException extends Exception {

    private final String name;
    private final MonitorFlag<?> flag;

    public WrongMonitorFlagSyntaxException(MonitorFlag<?> flag) {
        this.name = Optional.ofNullable(
                MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::getName).orElse("Unknown");
        this.flag = flag;
    }

    public static WrongMonitorFlagSyntaxException wrongFlag(String flag) {
        return new WrongMonitorFlagSyntaxException(null) {

            @Override
            public String getFlag() {
                return flag;
            }

            @Override
            public String getMessage() {
                return "Couldn't find any flag with that name";
            }
        };
    }

    public String getFlag() {
        return name;
    }

    public String getMessage() {
        return flag.getWrongSyntaxMessage();
    }
}
