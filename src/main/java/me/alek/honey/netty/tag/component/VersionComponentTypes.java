package me.alek.honey.netty.tag.component;

import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VersionComponentTypes {

    public static final List<ComponentType<?>> NON_ENCODED_COMPONENTS = Collections.unmodifiableList(Arrays.asList(
            ComponentTypes.CREATIVE_SLOT_LOCK, ComponentTypes.MAP_POST_PROCESSING));
    public static final List<ComponentType<?>> V1_21COMPONENTS = Collections.singletonList(ComponentTypes.JUKEBOX_PLAYABLE);
    public static final List<ComponentType<?>> V1_21_2COMPONENTS = Collections.unmodifiableList(Arrays.asList(
            ComponentTypes.REPAIRABLE, ComponentTypes.ENCHANTABLE, ComponentTypes.CONSUMABLE, ComponentTypes.USE_COOLDOWN, ComponentTypes.USE_REMAINDER
    ));

    public static List<ComponentType<?>> getBaseComponents() {
        List<ComponentType<?>> types = new ArrayList<>(ComponentTypes.values());
        types.removeAll(NON_ENCODED_COMPONENTS);
        types.removeAll(V1_21COMPONENTS);
        types.removeAll(V1_21_2COMPONENTS);
        return types;
    }

    public static List<ComponentType<?>> getComponents(NetProtocol protocol) {
        if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
            throw new IllegalArgumentException("Components for legacy version");
        }
        List<ComponentType<?>> types = getBaseComponents();
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_21)) {
            types.addAll(V1_21COMPONENTS);
        }
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_21_2)) {
            types.addAll(V1_21_2COMPONENTS);
        }
        return types;
    }
}
