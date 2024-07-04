package me.alek.honey.compat.via;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.ServiceValidator;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.Array;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.LibraryVersion;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public enum ViaProtocol {

    v1_8(NetProtocol.v1_8, LegacyHolder.LEGACY ? "protocol1_8" : getPackageBetween(NetProtocol.v1_8, NetProtocol.v1_9, false), Array.from(PacketState.PLAY)),
    v1_9(NetProtocol.v1_9, getPackageBetween(NetProtocol.v1_8, NetProtocol.v1_9, false), Array.from(PacketState.PLAY)),
    v1_9_3(NetProtocol.v1_9_3, LegacyHolder.LEGACY ? "protocol1_9_3to1_9_1_2" : getPackageBetween(NetProtocol.v1_9_1, NetProtocol.v1_9_3, false), Array.from(PacketState.PLAY)),
    v1_12(NetProtocol.v1_12, getPackageBetween(NetProtocol.v1_11_1, NetProtocol.v1_12, false), Array.from(PacketState.PLAY)),
    v1_12_1(NetProtocol.v1_12_1, getPackageBetween(NetProtocol.v1_12, NetProtocol.v1_12_1, false), Array.from(PacketState.PLAY)),
    v1_13(NetProtocol.v1_13, getPackageBetween(NetProtocol.v1_12_2, NetProtocol.v1_13, false), Array.from(PacketState.PLAY)),
    v1_14(NetProtocol.v1_14, getPackageBetween(NetProtocol.v1_13_2, NetProtocol.v1_14, false), Array.from(PacketState.PLAY)),
    v1_14_4(NetProtocol.v1_14_4, getPackageBetween(NetProtocol.v1_14_3, NetProtocol.v1_14_4, false), Array.from(PacketState.PLAY)),
    v1_15(NetProtocol.v1_15, getPackageBetween(NetProtocol.v1_14_4, NetProtocol.v1_15, false), Array.from(PacketState.PLAY)),
    v1_16(NetProtocol.v1_16, getPackageBetween(NetProtocol.v1_15_2, NetProtocol.v1_16, false), Array.from(PacketState.PLAY)),
    v1_16_2(NetProtocol.v1_16_2, getPackageBetween(NetProtocol.v1_16_1, NetProtocol.v1_16_2, false), Array.from(PacketState.PLAY)),
    v1_17(NetProtocol.v1_17, getPackageBetween(NetProtocol.v1_16_4, NetProtocol.v1_17, false), Array.from(PacketState.PLAY)),
    v1_17_1(NetProtocol.v1_17_1, getPackageBetween(NetProtocol.v1_17, NetProtocol.v1_17_1, false), Array.from(PacketState.PLAY)),
    v1_18(NetProtocol.v1_18, getPackageBetween(NetProtocol.v1_17_1, NetProtocol.v1_18, false), Array.from(PacketState.PLAY)),
    v1_19(NetProtocol.v1_19, getPackageBetween(NetProtocol.v1_18_2, NetProtocol.v1_19, false), Array.from(PacketState.PLAY)),
    v1_19_1(NetProtocol.v1_19_1, getPackageBetween(NetProtocol.v1_19, NetProtocol.v1_19_1, false), Array.from(PacketState.PLAY)),
    v1_19_3(NetProtocol.v1_19_3, getPackageBetween(NetProtocol.v1_19_1, NetProtocol.v1_19_3, false), Array.from(PacketState.PLAY)),
    v1_19_4(NetProtocol.v1_19_4, getPackageBetween(NetProtocol.v1_19_3, NetProtocol.v1_19_4, false), Array.from(PacketState.PLAY)),
    v1_20_2(NetProtocol.v1_20_2, getPackageBetween(NetProtocol.v1_20, NetProtocol.v1_20_2, true), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
    v1_20_3(NetProtocol.v1_20_3, getPackageBetween(NetProtocol.v1_20_2, NetProtocol.v1_20_3, true), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
    v1_20_5(NetProtocol.v1_20_5, getPackageBetween(NetProtocol.v1_20_3, NetProtocol.v1_20_5, true), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
    ;

    private static class LegacyHolder {
        private static final boolean LEGACY;

        static {
            LibraryVersion version = new LibraryVersion(Bukkit.getPluginManager().getPlugin("ViaVersion").getDescription().getVersion());
            LEGACY = !version.isNewerThan(new LibraryVersion("4.10.2"));
        }
    }

    private static class DefaultConstantsHolder {

        private static final Enum<?>[][][] defaultEnumConstants = new Enum[PacketState.values().length][PacketBound.values().length][0];

    }

    private static final String PACKAGE = "com.viaversion.viaversion";

    static {
        ViaProtocol defaultProtocol = getViaProtocolFrom(NetProtocol.getProtocol());
        for (PacketBound bound : PacketBound.values()) {
            for (PacketState state : defaultProtocol.getUnsortedStates()[bound.ordinal()]) {
                DefaultConstantsHolder.defaultEnumConstants[state.ordinal()][bound.ordinal()] = defaultProtocol.getEnumConstants(bound, state);
            }
        }

        for (ViaProtocol protocol : values()) {
            protocol.load();
        }
    }

    private final NetProtocol protocol;
    private final String packageName;
    private final Array<PacketState>[] states = new Array[PacketBound.values().length];
    private final Array<PacketType>[] serverboundMappings = (Array<PacketType>[]) new Array[PacketState.values().length];
    private final Array<PacketType>[] clientboundMappings = (Array<PacketType>[]) new Array[PacketState.values().length];

    private static String getPackageBetween(NetProtocol lower, NetProtocol upper, boolean legacySubPackage) {
        if (LegacyHolder.LEGACY) {
            return "protocol" + upper + "to" + lower + (legacySubPackage ? ".packet" : "");
        } else {
            return "v" + lower + "to" + upper + ".packet";
        }
    }

    public static ViaProtocol getViaProtocolFrom(NetProtocol protocol) {
        for (ViaProtocol viaProtocol : values()) {
            NetProtocolRange range = NetProtocolRange.between(viaProtocol.getProtocol(), viaProtocol.getNextProtocol().getProtocol().getBefore());
            if (range.match(protocol) || viaProtocol.ordinal() == values().length - 1) {
                return viaProtocol;
            }
        }

        throw new IllegalStateException("Could not find any ViaVersion protocol for major version " + protocol);
    }

    ViaProtocol(NetProtocol protocol, String packageName, Array<PacketState> states) {
        this.protocol = protocol;
        this.packageName = packageName;

        for (PacketBound bound : PacketBound.values()) {
            this.states[bound.ordinal()] = states.copy();
        }
    }

    public void load() {
        for (PacketState state : states[0].compact()) {
            this.serverboundMappings[state.ordinal()] = getMappings(PacketBound.SERVER, state);
            this.clientboundMappings[state.ordinal()] = getMappings(PacketBound.CLIENT, state);
        }
    }


    private ViaProtocol getPreviousProtocol() {
        if (ordinal() == 0) {
            return this;
        }

        return values()[ordinal() - 1];
    }

    public ViaProtocol getNextProtocol() {
        if (ordinal() == values().length - 1) {
            return this;
        }

        return values()[ordinal() + 1];
    }

    private Array<PacketType> getMappings(PacketBound bound, PacketState state) {
        Array<PacketType> types = new Array<>();

        if (!hasState(bound, state)) {
            if (this.equals(ViaProtocol.v1_8)) {
                return types;
            }
            return getPreviousProtocol().getMappings(bound, state);
        }

        Enum<?>[] constants = getEnumConstants(bound, state);
        Enum<?>[] defaultConstants = DefaultConstantsHolder.defaultEnumConstants[state.ordinal()][bound.ordinal()];
        Arrays.stream(constants).forEach(c -> {
            Arrays.stream(defaultConstants).forEach(dc -> {
                if (c.toString().equals(dc.toString())) {
                    PacketType type = state.getTypes()[bound.ordinal()].get(dc.ordinal());
                    types.set(c.ordinal(), type);
                }
            });
        });

        if (types.isEmpty()) {
            return getPreviousProtocol().getMappings(bound, state);
        }

        return types;
    }

    public Enum<?>[] getEnumConstants(PacketBound bound, PacketState state) {
        try {
            String className = bound.getName() + (state.equals(PacketState.PLAY) ? "" : state.getName()) + "Packets" + protocol.toString();
            Class<Enum<?>> clazz = (Class<Enum<?>>) Reflection.getClass(PACKAGE + ".protocols." + packageName + "." + className);

            return clazz.getEnumConstants();
        } catch (Exception ex) {
            states[bound.ordinal()].remove(state);
        }

        if (getPreviousProtocol().hasState(bound, state)) {
            return getPreviousProtocol().getEnumConstants(bound, state);
        }

        return new Enum[0];
    }

    public NetProtocol getProtocol() {
        return protocol;
    }

    public boolean hasState(PacketBound bound, PacketState state) {
        return states[bound.ordinal()].contains(state);
    }

    public Array<PacketState>[] getUnsortedStates() {
        return states;
    }

    public Array<PacketType> getServerboundMappings(PacketState state) {
        return serverboundMappings[state.ordinal()];
    }

    public Array<PacketType> getClientboundMappings(PacketState state) {
        return clientboundMappings[state.ordinal()];
    }

    public static class ViaValidator implements ServiceValidator {

        public static final ServiceValidator VALIDATOR = new ViaValidator();

        private final LazyInit<Boolean> find = new LazyInit<>(() -> HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("ViaVersion") != null
                && HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("ViaVersion").isEnabled());

        @Override
        public LazyInit<Boolean> find() {
            return find;
        }
    }
}