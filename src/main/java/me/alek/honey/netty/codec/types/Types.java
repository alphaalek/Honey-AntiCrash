package me.alek.honey.netty.codec.types;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.util.protocol.NetProtocol;

public class Types {

    public static TypeSerializer.NoContext<Integer> VAR_INT = new VarInt();
    public static TypeSerializer<String, TypeSerializer.TypeContext.OneArg<Integer>> UTF8 = new Utf8String();

    public static TypeSerializer.In<ComponentMap, TypeSerializer.TypeContext.TwoArgs<ItemType, NetProtocol>> IN_COMPONENT_MAP = new ComponentMap.InSerializer();
    public static TypeSerializer.Out<ComponentMap, TypeSerializer.TypeContext.TwoArgs<ItemType, NetProtocol>> OUT_COMPONENT_MAP = new ComponentMap.OutSerializer();
    public static TypeSerializer<WrappedImmutableItemStack, TypeSerializer.TypeContext.TwoArgs<CodecVisitor, DecodeContext.StackContext>> ITEM_STACK = new WrappedImmutableItemStack.Serializer();
    public static TypeSerializer<AbstractTag<?, ?, ?>, TypeSerializer.TypeContext.OneArg<CodecVisitor>> NBT = new NBTFactory.Serializer();
}
