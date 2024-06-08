package me.alek.honey.netty.api;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.structure.converters.JavaConverter;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface ModifiablePacketContainer {

    IStructureModifier<Double> getDoubles();

    IStructureModifier<Long> getLongs();

    IStructureModifier<Integer> getInts();

    IStructureModifier<Short> getShorts();

    IStructureModifier<Float> getFloats();

    IStructureModifier<Byte> getBytes();

    IStructureModifier<Boolean> getBooleans();

    IStructureModifier<String> getStrings();

    IStructureModifier<Object> getObjects();

    IStructureModifier<Object> getAll();

    IStructureModifier<UUID> getUUIDS();

    IStructureModifier<String[]> getStringArrays();

    IStructureModifier<long[]> getLongArrays();

    IStructureModifier<int[]> getIntArrays();

    IStructureModifier<short[]> getShortArrays();

    IStructureModifier<byte[]> getByteArrays();

    IStructureModifier<ItemStack> getItems();

    IStructureModifier<WrappedGameProfile> getGameProfiles();

    IStructureModifier<PacketDataSerializer> getDataSerializers();

    IStructureModifier<WrappedBlockPosition> getBlockPositions();

    <T> IStructureModifier<T> getObjects(Class<T> target);

    <T> IStructureModifier<T> getModifier(Class<T> clazz);

    <T, C> IStructureModifier<C> getModifier(Class<T> clazz, JavaConverter<T, C> converter, Object... params);
}
