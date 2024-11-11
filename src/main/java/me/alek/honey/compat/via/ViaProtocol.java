package me.alek.honey.compat.via;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.ServiceValidator;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.util.Array;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.LibraryVersion;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public enum ViaProtocol {

    v1_8(NetProtocol.v1_8, LegacyHolder.LEGACY ? "protocol1_8" : getPackageBetween(NetProtocol.v1_8, NetProtocol.v1_9)),
    v1_9(NetProtocol.v1_9, getPackageBetween(NetProtocol.v1_8, NetProtocol.v1_9)),
    v1_9_3(NetProtocol.v1_9_3, LegacyHolder.LEGACY ? "protocol1_9_3to1_9_1_2" : getPackageBetween(NetProtocol.v1_9_1, NetProtocol.v1_9_3, false)),
    v1_12(NetProtocol.v1_12, getPackageBetween(NetProtocol.v1_11_1, NetProtocol.v1_12)),
    v1_12_1(NetProtocol.v1_12_1, getPackageBetween(NetProtocol.v1_12, NetProtocol.v1_12_1)),
    v1_13(NetProtocol.v1_13, getPackageBetween(NetProtocol.v1_12_2, NetProtocol.v1_13)),
    v1_14(NetProtocol.v1_14, getPackageBetween(NetProtocol.v1_13_2, NetProtocol.v1_14)),
    v1_14_4(NetProtocol.v1_14_4, getPackageBetween(NetProtocol.v1_14_3, NetProtocol.v1_14_4)),
    v1_15(NetProtocol.v1_15, getPackageBetween(NetProtocol.v1_14_4, NetProtocol.v1_15)),
    v1_16(NetProtocol.v1_16, getPackageBetween(NetProtocol.v1_15_2, NetProtocol.v1_16)),
    v1_16_2(NetProtocol.v1_16_2, getPackageBetween(NetProtocol.v1_16_1, NetProtocol.v1_16_2)),
    v1_17(NetProtocol.v1_17, getPackageBetween(NetProtocol.v1_16_4, NetProtocol.v1_17)),
    v1_17_1(NetProtocol.v1_17_1, getPackageBetween(NetProtocol.v1_17, NetProtocol.v1_17_1)),
    v1_18(NetProtocol.v1_18, getPackageBetween(NetProtocol.v1_17_1, NetProtocol.v1_18)),
    v1_19(NetProtocol.v1_19, getPackageBetween(NetProtocol.v1_18_2, NetProtocol.v1_19)),
    v1_19_1(NetProtocol.v1_19_1, getPackageBetween(NetProtocol.v1_19, NetProtocol.v1_19_1)),
    v1_19_3(NetProtocol.v1_19_3, getPackageBetween(NetProtocol.v1_19_1, NetProtocol.v1_19_3)),
    v1_19_4(NetProtocol.v1_19_4, getPackageBetween(NetProtocol.v1_19_3, NetProtocol.v1_19_4)),
    v1_20_2(NetProtocol.v1_20_2, getPackageBetween(NetProtocol.v1_20, NetProtocol.v1_20_2, true)),
    v1_20_3(NetProtocol.v1_20_3, getPackageBetween(NetProtocol.v1_20_2, NetProtocol.v1_20_3, true)),
    v1_20_5(NetProtocol.v1_20_5, getPackageBetween(NetProtocol.v1_20_3, NetProtocol.v1_20_5, true)),
    ;

    private static class LegacyHolder {
        private static final boolean LEGACY;

        static {
            LibraryVersion version = new LibraryVersion(Bukkit.getPluginManager().getPlugin("ViaVersion").getDescription().getVersion());
            LEGACY = !version.isNewerThan(new LibraryVersion("4.10.2"));
        }
    }

    private static final String PACKAGE = "com.viaversion.viaversion";
    private final NetProtocol protocol;
    private final String packageName;
    private final Array<PacketType>[] serverboundTypes = (Array<PacketType>[]) new Array[PacketState.values().length];
    private final Array<PacketType>[] clientboundTypes = (Array<PacketType>[]) new Array[PacketState.values().length];

    private static String getPackageBetween(NetProtocol lower, NetProtocol upper, boolean legacySubPackage) {
        if (LegacyHolder.LEGACY) {
            return "protocol" + upper + "to" + lower + (legacySubPackage ? ".packet" : "");
        } else {
            return "v" + lower + "to" + upper + ".packet";
        }
    }

    private static String getPackageBetween(NetProtocol lower, NetProtocol upper) {
        return getPackageBetween(lower, upper, false);
    }

    public static ViaProtocol getViaProtocolFrom(NetProtocol protocol) {
        List<ViaProtocol> protocols = Arrays.asList(values());
        Collections.reverse(protocols);

        for (ViaProtocol viaProtocol : protocols) {
            if (viaProtocol.getProtocol().isOlderThanOrEqual(protocol)) {
                return viaProtocol;
            }
        }

        throw new IllegalStateException("Could not find any ViaVersion protocol for version " + protocol);
    }

    static {
        for (ViaProtocol protocol : values()) {
            protocol.load();
        }
    }

    ViaProtocol(NetProtocol protocol, String packageName) {
        this.protocol = protocol;
        this.packageName = packageName;
    }

    private void load() {
        for (PacketState state : PacketState.values()) {
            if (state == PacketState.ANY || state == PacketState.UNKNOWN) {
                continue;
            }

            this.serverboundTypes[state.ordinal()] = initTypes(PacketBound.SERVER, state);
            this.clientboundTypes[state.ordinal()] = initTypes(PacketBound.CLIENT, state);

            Bukkit.getLogger().info("Loaded " + state + " " + this + " " + this.serverboundTypes[state.ordinal()]);
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

    private Array<PacketType> initTypes(PacketBound bound, PacketState state) {
        Array<PacketType> types = new Array<>();

        Enum<?>[] constants = getEnumConstants(bound, state);
        for (Enum<?> constant : constants) {
            PacketType type = PacketTypes.getTypeFromKey(this.protocol, state, bound, constant.name().toLowerCase());
            if (type == null) {
                throw new IllegalStateException("Could not get packet type from key " + constant.name().toLowerCase());
            }

            types.set(constant.ordinal(), type);
        }

        if (types.isEmpty() && ordinal() > 0) {
            return getPreviousProtocol().getTypes(bound, state);
        }

        return types;
    }

    private Enum<?>[] getEnumConstants(PacketBound bound, PacketState state) {
        try {
            String className = bound.getName() + (state.equals(PacketState.PLAY) ? "" : state.getName()) + "Packets" + protocol.toString();
            Class<Enum<?>> clazz = Reflection.getClass(PACKAGE + ".protocols." + packageName + "." + className);

            return clazz.getEnumConstants();
        } catch (Exception ex) {
            if (ordinal() == 0) {
                return getBaseEnumConstants(bound, state);
            }

            return getPreviousProtocol().getEnumConstants(bound, state);
        }
    }

    private Enum<?>[] getBaseEnumConstants(PacketBound bound, PacketState state) {
        try {
            String className = bound.getName() + (state.equals(PacketState.PLAY) ? "" : state.getName()) + "Packets";
            Class<Enum<?>> clazz = Reflection.getClass(PACKAGE + ".protocols.base." + className);

            return clazz.getEnumConstants();
        } catch (Exception ex) {
            return new Enum[0];
        }
    }

    public NetProtocol getProtocol() {
        return protocol;
    }

    public Array<PacketType> getTypes(PacketBound bound, PacketState state) {
        if (bound.equals(PacketBound.CLIENT)) {
            return getClientboundTypes(state);
        } else if (bound.equals(PacketBound.SERVER)) {
            return getServerboundTypes(state);
        } else {
            throw new IllegalArgumentException("Invalid bound");
        }
    }

    public Array<PacketType> getServerboundTypes(PacketState state) {
        return serverboundTypes[state.ordinal()];
    }

    public Array<PacketType> getClientboundTypes(PacketState state) {
        return clientboundTypes[state.ordinal()];
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