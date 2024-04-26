package me.alek.honey.monitor;

import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;

public class MonitorFlagParser {

    public static List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
        if (args.length == 0) {
            return new ArrayList<>();
        }

        List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags = new ArrayList<>();
        int readIndex = 0;

        while (readIndex < args.length) {
            String argument = args[readIndex++];
            if (argument.startsWith("--")) {

                String flagString = argument.toLowerCase().substring(2);
                MonitorFlag.Flag flagType = MonitorFlag.Flag.getFlagByName(flagString);
                if (flagType == null) {
                    throw WrongMonitorFlagSyntaxException.wrongFlag(flagString);
                }

                @SuppressWarnings("unchecked")
                MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag =
                        (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>) flagType.createNew();
                readIndex += flag.check(args, readIndex);
                flags.add(flag);
            }
        }
        return flags;
    }

    public static MonitorFlag.Flag getFlag(MonitorFlag<?> flag) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends MonitorFlag<?>> clazz = (Class<? extends MonitorFlag<?>>) flag.getClass();

            return clazz.getAnnotation(MonitorFlag.FlagAnnotation.class).value();
        } catch (Exception ex) {
            return null;
        }
    }
}
