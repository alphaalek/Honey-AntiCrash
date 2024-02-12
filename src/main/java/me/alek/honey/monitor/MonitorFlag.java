package me.alek.honey.monitor;

import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NetProtocol;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface MonitorFlag<T> {

    enum Flag {
        PLAYER("player", "--player [player name]", PlayerFlag::new),
        TYPE("type", "--type [packet type]", TypeFlag::new),

        ;

        private final String name;
        private final String usage;
        private final Supplier<MonitorFlag<?>> monitorFlagSupplier;

        Flag(String name, String usage, Supplier<MonitorFlag<?>> monitorFlagSupplier) {
            this.name = name;
            this.usage = usage;
            this.monitorFlagSupplier = monitorFlagSupplier;
        }

        public String getName() {
            return this.name;
        }

        public String getUsage() {
            return this.usage;
        }

        public MonitorFlag<?> createNew() {
            return monitorFlagSupplier.get();
        }

        private static final Map<String, Flag> flagsLookup = new HashMap<>();

        static {
            for (Flag flag : Flag.values()) {
                flagsLookup.put(flag.getName(), flag);
            }
        }

        public static Flag getFlagByName(String name) {
            return flagsLookup.get(name);
        }

        public static void checkSyntax(String[] args, int readIndex, MonitorFlag<?> flag, int takes) throws WrongMonitorFlagSyntaxException {
            if (readIndex - 1 + takes >= args.length) {
                throw new WrongMonitorFlagSyntaxException(flag);
            }
        }

        public static Flag getFlag(MonitorFlag<?> flag) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends MonitorFlag<?>> clazz = (Class<? extends MonitorFlag<?>>) flag.getClass();

                return clazz.getAnnotation(FlagAnnotation.class).value();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException;

    boolean test(T input);

    default void append(T input, List<String> data) {

    }

    default String getWrongSyntaxMessage() {
        return "You have to choose an argument";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface FlagAnnotation {
        Flag value();
    }

    @FlagAnnotation(Flag.PLAYER)
    class PlayerFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private String playerName;

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            this.playerName = args[readIndex];
            return 1;
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.getPlayer().getName().equalsIgnoreCase(playerName);
        }
    }

    @FlagAnnotation(Flag.TYPE)
    class TypeFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private String type;

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            this.type = args[readIndex];
            {
                PacketTypeEnum packetType = PacketWrapperInvokerFactory.getPacketTypeForNameIgnoreCase(type);
                if (packetType != null) {
                    this.type = packetType.toString();
                    return 1;
                }
            }
            for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketState.PLAY, PacketBound.CLIENT)) {
                if (packetType.toString().equalsIgnoreCase(type)) {
                    return 1;
                }
            }
            throw new WrongMonitorFlagSyntaxException(this);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.getType().toString().equalsIgnoreCase(type);
        }

        public String getWrongSyntaxMessage() {
            return "Couldn't find any packet type matching \"" + type + "\"";
        }
    }
}
