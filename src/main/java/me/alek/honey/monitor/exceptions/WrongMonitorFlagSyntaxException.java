package me.alek.honey.monitor.exceptions;

import me.alek.honey.monitor.LogicalOperator;
import me.alek.honey.monitor.MonitorFlag;
import me.alek.honey.monitor.MonitorFlagParser;

import java.util.Optional;

public abstract class WrongMonitorFlagSyntaxException extends Exception {

    public static WrongMonitorFlagSyntaxException wrongFlagUsage(MonitorFlag<?> flag) {
        return new WrongMonitorFlagSyntaxException() {

            @Override
            public String getType() {
                return "monitor flag " + Optional.ofNullable(
                        MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::getName).orElse("Unknown");
            }

            @Override
            public String getMessage() {
                return flag.getWrongSyntaxMessage();
            }
        };
    }

    public static WrongMonitorFlagSyntaxException wrongFlag(String flag) {
        return new WrongMonitorFlagSyntaxException() {

            @Override
            public String getType() {
                return flag;
            }

            @Override
            public String getMessage() {
                return "Couldn't find any flag with that name";
            }
        };
    }

    public static WrongMonitorFlagSyntaxException wrongOperatorUsage(LogicalOperator operator, String message) {
        return new WrongMonitorFlagSyntaxException() {

            @Override
            public String getType() {
                return "logical operator " + operator.name();
            }

            @Override
            public String getMessage() {
                return message;
            }
        };
    }

    public abstract String getType();

    public abstract String getMessage();
}
