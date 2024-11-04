package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.AbstractTag;
import me.alek.honey.netty.nbt.NBTFactory;

public class Types {

    public static Type<Integer> VAR_INT = new VarInt();
    public static Type<String> UTF8 = new Utf8String();

    public static Type.ProtocolType<WrappedImmutableItemStack, Type.In<WrappedImmutableItemStack>> ITEM_STACK = WrappedImmutableItemStack.ProtocolType::forProtocol;
    public static Type.ProtocolType<AbstractTag<?, ?, ?>, Type.In<AbstractTag<?, ?, ?>>> NBT = NBTFactory.ProtocolType::forProtocol;
}
