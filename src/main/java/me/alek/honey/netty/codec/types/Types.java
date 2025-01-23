package me.alek.honey.netty.codec.types;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;

public class Types {

    public static Type.NoContext<Integer> VAR_INT = new VarInt();
    public static Type<String, Type.TypeContext.OneArg<Integer>> UTF8 = new Utf8String();

    public static Type.ProtocolType<ComponentMap, Type.TypeContext.OneArg<ItemType>, ComponentMap.ProtocolType> COMPONENT_MAP = ComponentMap.ProtocolType::forProtocol;
    public static Type.ProtocolType<WrappedImmutableItemStack, Type.TypeContext.Empty, WrappedImmutableItemStack.ProtocolType> ITEM_STACK = WrappedImmutableItemStack.ProtocolType::forProtocol;
    public static Type.ProtocolType<AbstractTag<?, ?, ?>, Type.TypeContext.Empty, NBTFactory.ProtocolType> NBT = NBTFactory.ProtocolType::forProtocol;
}
