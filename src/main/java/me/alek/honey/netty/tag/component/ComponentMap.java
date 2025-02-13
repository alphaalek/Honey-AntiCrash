package me.alek.honey.netty.tag.component;

import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.TypeSerializer;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.visitor.ComponentMapVisitor;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.PEConverter;
import me.alek.honey.netty.tag.nbt.TagTypeCollection;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ComponentMap implements ItemTag {

    public static class Serializer implements TypeSerializer<ComponentMap, TypeSerializer.TypeContext.OneArg<ComponentMapVisitor>> {

        @SuppressWarnings("unchecked")
        @Override
        public ComponentMap read(PacketDataSerializer serializer, TypeSerializer.TypeContext.OneArg<ComponentMapVisitor> context) {
            int present = serializer.readVarInt();
            int absent = serializer.readVarInt();
            if (present == 0 && absent == 0) {
                return null;
            }

            if (absent < 0 || present < 0) {
                throw new DecoderException("Invalid component map: absent " + absent + ", present " + present);
            }
            ComponentMapVisitor.In visitor = (ComponentMapVisitor.In) context.get();
            visitor.visitAt(serializer, present, absent);

            NetProtocol protocol = visitor.protocol();
            NetProtocol protocolCreated = visitor.protocolCreated();
            List<ComponentType<?>> available = VersionComponentTypes.getComponents(protocolCreated);

            ItemType itemType = visitor.type();
            List<Val<?>> bases = itemType.getComponents().getDelegate().entrySet().stream()
                    .map(e -> new ComponentMap.Val<>((ComponentType<Object>) e.getKey(), e.getValue(), ComponentSource.BASE))
                    .collect(Collectors.toList());
            if (absent > bases.size()) {
                throw new DecoderException("Too many absent tags: " + absent + ", bases " + bases.size());
            }
            if (present > available.size() - bases.size()) {
                throw new DecoderException("Too many present tags: " + present + ", available " + (available.size() - bases.size()));
            }

            PacketWrapper<?> wrapper = visitor.createUniversalWrapper(serializer);

            ComponentMap map = new ComponentMap(bases);
            for (int i = 0; i < present; i++) {
                ComponentType<Object> type = readType(protocol, serializer);
                if (available.contains(type) &&
                        Optional.ofNullable(map.get(type))
                                .map(Val::source)
                                .orElse(ComponentSource.BASE)
                                .equals(ComponentSource.BASE)) {
                    WrapperComponentType<Object> wrapperType = (WrapperComponentType<Object>) WrapperComponentType.REGISTRY.get(type);
                    Object val;
                    try {
                        val = wrapperType.visitAt(visitor, serializer);
                    } catch (FallbackDefaultCodecException ex) {
                        val = type.read(wrapper);
                        // convert nbt to honey implementation, as it's superior ^_^
                        if (val instanceof NBTCompound) {
                            AbstractTag<?, ?, ?> tag = PEConverter.convertPETag((NBTCompound) val);
                            if (tag.getType() != CompoundTag.TYPE.get()) {
                                throw new DecoderException("Invalid nbt: expected compound tag, but got: " + tag.getType());
                            }
                            val = tag;
                        }
                    }
                    wrapperType.visit(visitor, val);
                    map.set(type, val);
                } else {
                    throw new DecoderException("Invalid present component type: " + type);
                }
            }
            for (int i = 0; i < absent; i++) {
                ComponentType<?> type = readType(protocol, serializer);
                if (map.has(type) && available.contains(type)) {
                    visitor.visitAbsent(type);
                    map.remove(type);
                } else {
                    throw new DecoderException("Invalid absent component type: " + type);
                }
            }

            visitor.visitFinal(map);

            return map;
        }

        @SuppressWarnings("unchecked")
        private ComponentType<Object> readType(NetProtocol protocol, PacketDataSerializer serializer) {
            int id = serializer.readVarInt();
            return (ComponentType<Object>)
                    ComponentTypes.getRegistry().getById(protocol.toPECVersion(), id);
        }

        @SuppressWarnings("unchecked")
        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, ComponentMap map, TypeContext.OneArg<ComponentMapVisitor> context) {
            ComponentMapVisitor.Out visitor = (ComponentMapVisitor.Out) context.get();
            ItemType itemType = visitor.type();
            List<Val<?>> defaultBases = itemType.getComponents().getDelegate().entrySet().stream()
                    .map(e -> new ComponentMap.Val<>((ComponentType<Object>) e.getKey(), e.getValue(), ComponentSource.BASE))
                    .collect(Collectors.toList());
            if (map.delegate.values().containsAll(defaultBases) && map.delegate.size() == defaultBases.size()) {
                buf.writeShort(0);
                return buf;
            }
            NetProtocol protocol = visitor.protocol();

            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buf);
            wrapper.setClientVersion(protocol.toPECVersion());
            wrapper.setServerVersion(protocol.toPESVersion());

            List<ComponentType<?>> bases = map.getAllBases().stream()
                    .map(Val::type)
                    .collect(Collectors.toList());

            // find patched types
            List<Val<?>> patches = map.getAllPatches();
            buf.writeVarInt(patches.size());

            // find absent types
            List<Val<?>> absents = defaultBases.stream()
                    .filter(b -> !bases.contains(b.type()))
                    .collect(Collectors.toList());
            buf.writeVarInt(absents.size());

            // write present patches
            for (Val<?> patch : patches) {
                writeType(wrapper, protocol, buf, (Val<Object>) patch);
            }

            // write absent types
            for (Val<?> absent : absents) {
                buf.writeVarInt(absent.type().getId(wrapper.getClientVersion()));
            }

            return buf;
        }

        private void writeType(PacketWrapper<?> wrapper, NetProtocol protocol, PacketDataSerializer buf, Val<Object> val) {
            buf.writeVarInt(val.type().getId(protocol.toPECVersion()));

            // convert nbt back to PE implementation
            Object del = val.get();
            if (del instanceof CompoundTag) {
                del = PEConverter.convertTag((CompoundTag) del);
            }

            val.type().write(wrapper, del);
        }
    }

    public static final ComponentMap EMPTY = new ComponentMap();

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
