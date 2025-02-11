package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.TypeSerializer;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.component.visitor.ComponentVisitor;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;

public class Types {

    public static TypeSerializer.NoContext<Integer> VAR_INT = new VarInt();
    public static TypeSerializer<String, TypeSerializer.TypeContext.OneArg<Integer>> UTF8 = new Utf8String();

    public static Rewritable.Serializer<WrappedImmutableItemStack, TypeSerializer.TypeContext.TwoArgs<CodecVisitor.Dynamic<?, ?>, CodecContext>> ITEM_STACK = new WrappedImmutableItemStack.Serializer();
    public static TypeSerializer<ComponentMap, TypeSerializer.TypeContext.OneArg<ComponentVisitor>> COMPONENT_MAP = new ComponentMap.Serializer();
    public static TypeSerializer<AbstractTag<?, ?, ?>, TypeSerializer.TypeContext.OneArg<NBTVisitor>> NBT = new NBTFactory.Serializer();
}
