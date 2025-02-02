package me.alek.honey.netty.tag.component;

import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBufInputStream;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComponentMap implements ItemTag {

    public static class ProtocolType implements Type.In<ComponentMap, Type.TypeContext.OneArg<ItemType>> {

        private static final

        public static ComponentMap.ProtocolType forProtocol(NetProtocol protocol) {
            return new ComponentMap.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ComponentMap read(PacketDataSerializer serializer, TypeContext.OneArg<ItemType> context) {
            int present = serializer.readVarInt();
            int absent = serializer.readVarInt();
            if (present == 0 && absent == 0) {
                return null;
            }

            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(serializer);
            wrapper.setClientVersion(this.protocol.toPECVersion());
            wrapper.setServerVersion(this.protocol.toPESVersion());

            List<Val<?>> bases = context.get().getComponents().getDelegate().entrySet().stream()
                    .map(e -> new ComponentMap.Val<>((ComponentType<Object>) e.getKey(), e.getValue(), ComponentSource.BASE))
                    .collect(Collectors.toList());
            ComponentMap map = new ComponentMap(bases);
            for (int i = 0; i < present; i++) {
                ComponentType<Object> type = readType(serializer);
                Object val = type.read(wrapper);
                map.set(type, val);
            }
            for (int i = 0; i < absent; i++) {
                map.remove(readType(serializer));
            }

            return map;
        }

        @SuppressWarnings("unchecked")
        private ComponentType<Object> readType(PacketDataSerializer serializer) {
            int id = serializer.readVarInt();
            return (ComponentType<Object>)
                    ComponentTypes.getRegistry().getById(this.protocol.toPECVersion(), id);
        }
    }


    private static final ComponentMap EMPTY = new ComponentMap();

    private final Map<ComponentType<?>, Val<?>> delegate = new HashMap<>();

    public ComponentMap(List<Val<?>> bases) {
        this.delegate.putAll(bases.stream().collect(Collectors.toMap(Val::type, v -> v)));
    }

    public ComponentMap() {

    }

    @Override
    public Representation<ComponentMap> asComponentMap() {
        return Representation.as(this);
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @SuppressWarnings("unchecked")
    public <T> Val<T> get(ComponentType<T> type) {
        return (Val<T>) this.delegate.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVal(ComponentType<T> type) {
        Val<T> val = (Val<T>) this.delegate.get(type);
        if (val == null) {
            return null;
        }

        return val.delegate;
    }

    public List<Val<?>> getAllOf(ComponentSource source) {
        return this.delegate.values().stream().filter(v -> v.source.equals(source)).collect(Collectors.toList());
    }

    public List<Val<?>> getAllPatches() {
        return getAllOf(ComponentSource.PATCH);
    }

    public List<Val<?>> getAllBases() {
        return getAllOf(ComponentSource.BASE);
    }

    public <T> void set(ComponentType<T> type, T value) {
        this.delegate.put(type, new Val<>(type, value, ComponentSource.PATCH));
    }

    public boolean has(ComponentType<?> type) {
        return this.delegate.containsKey(type);
    }

    public boolean has(ComponentType<?> type, ComponentSource source) {
        return has(type) && this.delegate.get(type).source().equals(source);
    }

    public void remove(ComponentType<?> type) {
        this.delegate.remove(type);
    }

    public static class Val<T> {

        private final ComponentType<T> type;
        private final T delegate;
        private final ComponentSource source;

        public Val(ComponentType<T> type, T delegate, ComponentSource source) {
            this.type = type;
            this.delegate = delegate;
            this.source = source;
        }

        public ComponentType<T> type() {
            return this.type;
        }

        public T get() {
            return this.delegate;
        }

        public ComponentSource source() {
            return this.source;
        }
    }
}
