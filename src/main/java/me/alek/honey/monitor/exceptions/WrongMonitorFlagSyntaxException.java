package me.alek.honey.monitor.exceptions;

import me.alek.honey.monitor.MonitorFlag;

import java.util.Optional;

public class WrongMonitorFlagSyntaxException extends Exception {

    private final String name;
    private final MonitorFlag<?> flag;

    public WrongMonitorFlagSyntaxException(MonitorFlag<?> flag) {
        this.name = Optional.ofNullable(
                MonitorFlag.Flag.getFlag(flag)).map(MonitorFlag.Flag::getName).orElse("Unknown");
        this.flag = flag;
    }

    public String getFlag() {
        return name;
    }

    public String getMessage() {
        return flag.getWrongSyntaxMessage();
    }
}
