package me.alek.honey.netty.api.packet.container;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface ModifiablePacketContainer{

    IStructureModifier<Double> getDoubles();

    IStructureModifier<Long> getLongs();

    IStructureModifier<Integer> getInts();

    IStructureModifier<Short> getShorts();

    IStructureModifier<Float> getFloats();

    IStructureModifier<Byte> getBytes();

    IStructureModifier<Boolean> getBooleans();

    IStructureModifier<String> getStrings();

    IStructureModifier<Object> getObjects();

    IStructureModifier<?> getAll();

    IStructureModifier<UUID> getUUIDS();

    IStructureModifier<String[]> getStringArrays();

    IStructureModifier<long[]> getLongArrays();

    IStructureModifier<int[]> getIntArrays();

    IStructureModifier<short[]> getShortArrays();

    IStructureModifier<byte[]> getByteArrays();

    IStructureModifier<ItemStack> getItems();

    IStructureModifier<WrappedPacketDataSerializer> getDataSerializers();

    IStructureModifier<WrappedBlockPosition> getBlockPositions();

    <T> IStructureModifier<T> getObjects(Class<T> target);

}
