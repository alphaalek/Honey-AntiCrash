package me.alek.honey.netty.codec;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.HashMap;
import java.util.Map;

public interface CodecVisitor {

    interface Factory<C extends CodecVisitor> {

        Map<ItemType, Factory<?>> IN_REGISTRY = new HashMap<>();
        Map<ItemType, Factory<?>> OUT_REGISTRY = new HashMap<>();

        C create(NetProtocol protocol, ItemType type);
    }

    static CodecVisitor of(NetProtocol protocol, boolean replaceByRegistered) {
        return new CodecVisitor() {
            @Override
            public NetProtocol protocol() {
                return protocol;
            }

            @Override
            public boolean replacedByRegistried() {
                return replaceByRegistered;
            }
        };
    }

    NetProtocol protocol();

    boolean replacedByRegistried();
}
