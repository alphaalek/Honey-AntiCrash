package me.alek.honey.netty.packet;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;

import java.lang.reflect.Field;
import java.util.*;

public class PacketBundleFilters {

    public static PacketBundleFilter BLOCK_DIG_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.BLOCK_DIG);
    public static PacketBundleFilter BLOCK_PLACE_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.BLOCK_PLACE);
    public static PacketBundleFilter CHAT_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.CHAT);
    public static PacketBundleFilter CREATIVE_SLOT_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.SET_CREATIVE_SLOT);
    public static PacketBundleFilter CUSTOM_PAYLOAD_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.CUSTOM_PAYLOAD);
    public static PacketBundleFilter FLOOD_FILTER = PacketBundleFilterFactory.createLenientFilter(10);
    public static PacketBundleFilter HELD_ITEM_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.HELD_ITEM_SLOT);
    public static PacketBundleFilter LENIENT_LONG_FILTER = PacketBundleFilterFactory.createLenientFilter(10);
    public static PacketBundleFilter LENIENT_SINGLETON_FILTER = PacketBundleFilterFactory.createLenientSingletonFilter();
    public static PacketBundleFilter LONG_POSITION_FILTER = PacketBundleFilterFactory.createFilter(10, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
    public static PacketBundleFilter SHORT_POSITION_FILTER = PacketBundleFilterFactory.createFilter(2, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
    public static PacketBundleFilter LONG_ROTATION_FILTER = PacketBundleFilterFactory.createFilter(10, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
    public static PacketBundleFilter SHORT_ROTATION_FILTER = PacketBundleFilterFactory.createFilter(2, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
    public static PacketBundleFilter LONG_TAB_COMPLETE_FILTER = PacketBundleFilterFactory.createFilter(10, PacketType.Play.Client.TAB_COMPLETE);
    public static PacketBundleFilter SINGLETON_TAB_COMPLETE_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.TAB_COMPLETE);
    public static PacketBundleFilter UPDATE_SIGN_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.UPDATE_SIGN);
    public static PacketBundleFilter USE_ENTITY_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.USE_ENTITY);
    public static PacketBundleFilter WINDOW_CLICK_FILTER = PacketBundleFilterFactory.createSingletonFilter(PacketType.Play.Client.WINDOW_CLICK);

    public static Set<PacketTypeEnum> FILTER_ALL_PACKETS;

    static {
        updateFilterAllPackets();
    }

    public static void updateFilterAllPackets() {
        Set<PacketTypeEnum> packets = new HashSet<>();
        for (String ignoredPacket : ConfigurationOptions.PacketDump.VLS_FILTER_ALL) {
            packets.add(PacketWrapperInvokerFactory.getPacketTypeForName(ignoredPacket));
        }
        FILTER_ALL_PACKETS = packets;
    }

    public static Map<Set<PacketTypeEnum>, Integer> getPacketSaves() {
        Map<Set<PacketTypeEnum>, Integer> saves = new HashMap<>();
        for (PacketBundleFilter filter : getAllFilters()) {
            int size = filter.size();
            Set<PacketTypeEnum> packetTypes = Sets.newHashSet(filter.getAcceptedPacketTypes());

            if (packetTypes.size() == PacketBundleFilterFactory.getLenientFilterSize()) {
                for (PacketTypeEnum packetType : FILTER_ALL_PACKETS) {
                    packetTypes.remove(packetType);
                }
            }
            if (!packetTypes.isEmpty() &&
                    (!saves.containsKey(packetTypes) ||
                            (saves.containsKey(packetTypes) && saves.get(packetTypes) < size)
                    )
            )
                saves.put(packetTypes, size);
        }
        return saves;
    }

    public static List<PacketBundleFilter> getAllFilters() {
        List<PacketBundleFilter> filters = new ArrayList<>();
        for (Field field : PacketBundleFilters.class.getDeclaredFields()) {
            try {
                filters.add((PacketBundleFilter) field.get(null));
            } catch (Exception ex) {
                String name = field.getName();
                if (name.endsWith("_FILTER"))
                    HoneyPlugin.get().getLoggerService().getLogger().severe("Failed to get packet bundle filter: " + name.substring(0, name.length() - "_FILTER".length()));
            }
        }
        return filters;
    }
}
