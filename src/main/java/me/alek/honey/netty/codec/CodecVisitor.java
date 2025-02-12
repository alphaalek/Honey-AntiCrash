package me.alek.honey.netty.codec;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.alek.honey.netty.tag.component.visitor.CommonComponentMapInVisitor;
import me.alek.honey.netty.tag.component.visitor.ComponentMapVisitor;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.HashMap;
import java.util.Map;

public interface CodecVisitor {

    interface NBTDynamicMember<C extends NBTVisitor> {

        C createNBTVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type);
    }

    interface ComponentMapDynamicMember<C extends ComponentMapVisitor> {

        C createComponentMapVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type);
    }

    interface Dynamic<NBT extends NBTVisitor, CMP extends ComponentMapVisitor> extends NBTDynamicMember<NBT>, ComponentMapDynamicMember<CMP> {

        Dynamic<?, ?> IN_DEFAULT = of(NBTVisitor.In::createDefault, CommonComponentMapInVisitor::new, true);
        Dynamic<?, ?> OUT_DEFAULT = of(NBTVisitor.Out::createDefault, ComponentMapVisitor.Out::createDefault, true);

        Map<ItemType, Dynamic<?, ?>> IN_REGISTRY = new HashMap<>();
        Map<ItemType, Dynamic<?, ?>> OUT_REGISTRY = new HashMap<>();

        boolean replaceByRegistered();

        @SuppressWarnings("unchecked")
        default <C extends CodecVisitor> C create(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                return (C) createComponentMapVisitor(protocol, protocolCreated, type);
            } else {
                return (C) createNBTVisitor(protocol, protocolCreated, type);
            }
        }

        static <NBT extends NBTVisitor, CMP extends ComponentMapVisitor> Dynamic<NBT, CMP> of(NBTDynamicMember<NBT> nbtVisitorFactory, ComponentMapDynamicMember<CMP> componentVisitorFactory, boolean replaceByRegistered) {
            return new Dynamic<NBT, CMP>() {

                @Override
                public boolean replaceByRegistered() {
                    return replaceByRegistered;
                }

                @Override
                public CMP createComponentMapVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
                    return componentVisitorFactory.createComponentMapVisitor(protocol, protocolCreated, type);
                }

                @Override
                public NBT createNBTVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
                    return nbtVisitorFactory.createNBTVisitor(protocol, protocolCreated, type);
                }
            };
        }
    }

    static CodecVisitor of(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        return new CodecVisitor() {
            @Override
            public ItemType type() {
                return type;
            }

            @Override
            public NetProtocol protocol() {
                return protocol;
            }

            @Override
            public NetProtocol protocolCreated() {
                return protocolCreated;
            }
        };
    }

    default PacketWrapper<?> createUniversalWrapper(PacketDataSerializer serializer) {
        PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(serializer);
        wrapper.setClientVersion(protocol().toPECVersion());
        wrapper.setServerVersion(protocol().toPESVersion());
        return wrapper;
    }

    ItemType type();

    NetProtocol protocol();

    NetProtocol protocolCreated();
}
