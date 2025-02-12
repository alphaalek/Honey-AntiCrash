package me.alek.honey.netty.tag.nbt;

import com.github.retrooper.packetevents.protocol.nbt.*;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.array.GeneticArrayTypes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PEConverter {

    public static NBT convertTag(AbstractTag<?, ?, ?> tag) {
        if (tag.getType().getDataTypeClass().equals(List.class)) {
            List<NBT> array = tag.asArray().getValues().stream()
                    .map(PEConverter::convertTag).
                    collect(Collectors.toList());
            return new NBTList<>(getPEType(tag.getType()), array);
        } else if (tag.getType() == CompoundTag.TYPE.get()) {
            NBTCompound compoundTag = new NBTCompound();
            for (Map.Entry<String, AbstractTag<?, ?, ?>> entry : tag.asCompound().getValue().getDelegatedMap().entrySet()) {
                compoundTag.setTag(entry.getKey(), convertTag(entry.getValue()));
            }
            return compoundTag;
        } else if (tag.getType() == TagType.STRING) {
            return new NBTString(tag.<String>asPrimitive().getValue());
        } else if (tag.getType() == TagType.INT) {
            return new NBTInt(tag.<Integer>asPrimitive().getValue());
        } else if (tag.getType() == TagType.DOUBLE) {
            return new NBTDouble(tag.<Double>asPrimitive().getValue());
        } else if (tag.getType() == TagType.FLOAT) {
            return new NBTFloat(tag.<Float>asPrimitive().getValue());
        } else if (tag.getType() == TagType.LONG) {
            return new NBTLong(tag.<Long>asPrimitive().getValue());
        } else if (tag.getType() == TagType.SHORT) {
            return new NBTShort(tag.<Short>asPrimitive().getValue());
        } else if (tag.getType() == TagType.BYTE) {
            return new NBTByte(tag.<Byte>asPrimitive().getValue());
        } else if (tag.getType() == TagType.ARRAY_OF_INT) {
            return new NBTIntArray(tag.<int[]>asPrimitive().getValue());
        } else if (tag.getType() == TagType.ARRAY_OF_LONG) {
            return new NBTLongArray(tag.<long[]>asPrimitive().getValue());
        } else if (tag.getValue() == TagType.ARRAY_OF_BYTE) {
            return new NBTByteArray(tag.<byte[]>asPrimitive().getValue());
        } else if (tag.getType() == TagType.END) {
            return new NBTEnd();
        } else {
            throw new IllegalArgumentException("Couldn't convert tag: " + tag.getType());
        }
    }

    public static AbstractTag<?, ?, ?> convertPETag(NBT nbt) {
        if (nbt.getType() == NBTType.LIST) {
            NBTList<?> array = (NBTList<?>) nbt;
            List<AbstractTag<?, ?, ?>> tags = array.getTags().stream()
                    .map(PEConverter::convertPETag)
                    .collect(Collectors.toList());
            if (array.getTagsType() == NBTType.COMPOUND) {
                return new GeneticArrayTypes.ArrayOfCompoundTag(tags);
            } else if (array.getTagsType() == NBTType.LIST) {
                return new GeneticArrayTypes.ArrayOfArrayTag<>(tags);
            } else {
                return new ArrayTag<>(getType(array.getTagsType()), tags);
            }
        } else if (nbt.getType() == NBTType.COMPOUND) {
            NBTCompound compound = (NBTCompound) nbt;
            Map<String, AbstractTag<?, ?, ?>> tag = compound.getTags().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertPETag(entry.getValue())));
            return new CompoundTag(tag);
        } else if (nbt.getType() == NBTType.STRING) {
            return new PrimitiveTag<>(TagType.STRING, ((NBTString) nbt).getValue());
        } else if (nbt.getType() == NBTType.INT) {
            return new PrimitiveTag<>(TagType.INT, ((NBTInt) nbt).getAsInt());
        } else if (nbt.getType() == NBTType.DOUBLE) {
            return new PrimitiveTag<>(TagType.DOUBLE, ((NBTDouble) nbt).getAsDouble());
        } else if (nbt.getType() == NBTType.FLOAT) {
            return new PrimitiveTag<>(TagType.FLOAT, ((NBTFloat) nbt).getAsFloat());
        } else if (nbt.getType() == NBTType.LONG) {
            return new PrimitiveTag<>(TagType.LONG, ((NBTLong) nbt).getAsLong());
        } else if (nbt.getType() == NBTType.SHORT) {
            return new PrimitiveTag<>(TagType.SHORT, ((NBTShort) nbt).getAsShort());
        } else if (nbt.getType() == NBTType.BYTE) {
            return new PrimitiveTag<>(TagType.BYTE, ((NBTByte) nbt).getAsByte());
        } else if (nbt.getType() == NBTType.INT_ARRAY) {
            return new PrimitiveTag<>(TagType.ARRAY_OF_INT, ((NBTIntArray) nbt).getValue());
        } else if (nbt.getType() == NBTType.LONG_ARRAY) {
            return new PrimitiveTag<>(TagType.ARRAY_OF_LONG, ((NBTLongArray) nbt).getValue());
        } else if (nbt.getType() == NBTType.BYTE_ARRAY) {
            return new PrimitiveTag<>(TagType.ARRAY_OF_BYTE, ((NBTByteArray) nbt).getValue());
        } else if (nbt.getType() == NBTType.END) {
            return PrimitiveTag.END;
        } else {
            throw new IllegalArgumentException("Couldn't convert tag: " + nbt.getType());
        }
    }

    public static TagType getType(NBTType<?> type) {
        if (type == NBTType.LIST) {
            return ArrayTag.TYPE.get();
        } else if (type == NBTType.COMPOUND) {
            return CompoundTag.TYPE.get();
        } else if (type == NBTType.STRING) {
            return TagType.STRING;
        } else if (type == NBTType.INT) {
            return TagType.INT;
        } else if (type == NBTType.DOUBLE) {
            return TagType.DOUBLE;
        } else if (type == NBTType.FLOAT) {
            return TagType.FLOAT;
        } else if (type == NBTType.LONG) {
            return TagType.LONG;
        } else if (type == NBTType.SHORT) {
            return TagType.SHORT;
        } else if (type == NBTType.BYTE) {
            return TagType.BYTE;
        } else if (type == NBTType.INT_ARRAY) {
            return TagType.ARRAY_OF_INT;
        } else if (type == NBTType.LONG_ARRAY) {
            return TagType.ARRAY_OF_LONG;
        } else if (type == NBTType.BYTE_ARRAY) {
            return TagType.ARRAY_OF_BYTE;
        } else if (type == NBTType.END) {
            return TagType.END;
        } else {
            throw new IllegalArgumentException("Couldn't convert type: " + type.getNBTClass());
        }
    }

    @SuppressWarnings("unchecked")
    public static <N extends NBT> NBTType<N> getPEType(TagType type) {
        if (type.getDataTypeClass().equals(List.class)) {
            return (NBTType<N>) NBTType.LIST;
        } else if (type == CompoundTag.TYPE.get()) {
            return (NBTType<N>) NBTType.COMPOUND;
        } else if (type == TagType.STRING) {
            return (NBTType<N>) NBTType.STRING;
        } else if (type == TagType.INT) {
            return (NBTType<N>) NBTType.INT;
        } else if (type == TagType.DOUBLE) {
            return (NBTType<N>) NBTType.DOUBLE;
        } else if (type == TagType.FLOAT) {
            return (NBTType<N>) NBTType.FLOAT;
        } else if (type == TagType.LONG) {
            return (NBTType<N>) NBTType.LONG;
        } else if (type == TagType.SHORT) {
            return (NBTType<N>) NBTType.SHORT;
        } else if (type == TagType.BYTE) {
            return (NBTType<N>) NBTType.BYTE;
        } else if (type == TagType.ARRAY_OF_INT) {
            return (NBTType<N>) NBTType.INT_ARRAY;
        } else if (type == TagType.ARRAY_OF_LONG) {
            return (NBTType<N>) NBTType.LONG_ARRAY;
        } else if (type == TagType.ARRAY_OF_BYTE) {
            return (NBTType<N>) NBTType.BYTE_ARRAY;
        } else if (type == TagType.END) {
            return (NBTType<N>) NBTType.END;
        } else {
            throw new IllegalArgumentException("Couldn't convert type: " + type);
        }
    }
}
