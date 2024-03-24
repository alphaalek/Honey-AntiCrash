package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTElement;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTType;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.*;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

public class PacketExecutors {

    public static void register() {
        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        packetProcessor.addPacketExecutor(new CustomPayloadPacketExecutor(), PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

    private static class CustomPayloadPacketExecutor implements PacketExecutor<WrappedPlayInCustomPayload> {

        private static final Class<?> craftEventFactory = Reflection.getClass("{obc}.event.CraftEventFactory");
        private static final MethodInvoker<Object> handleBookEditEventInvoker = Reflection.getMethod(craftEventFactory, "handleEditBookEvent", NMS.getEntityPlayerClass(), NMS.getItemStackClass());

        @Override
        public boolean accepts(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            String channel = packetContainer.getPacket().getChannel();

            return NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2) &&
                    (channel.equals("MC|BEdit") || channel.equals("MC|BSign"));
        }

        @Override
        public Runnable getTask(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            WrappedPacketDataSerializer serializer = new WrappedPacketDataSerializer(packetContainer.getPacket().getDataSerializer().getHandle());
            ItemStack item = serializer.readItemStack();
            if (item == null) return null;

            serializer.release();

            NBTCompound nbt = NBTFactory.fromItem(item);

            switch (packetContainer.getPacket().getChannel()) {
                case "MC|BEdit":
                    if (!isValidQuillTag(nbt)) break;

                    return () -> handleMCBEdit(packetContainer, item, nbt);
                case "MC|BSign":
                    if (!isValidBookTag(nbt)) break;

                    return () -> handleMCBSign(packetContainer, item, nbt);
                default:
                    return null;
            }
            return () -> packetContainer.getPlayer().player.kickPlayer("Invalid book data!");
        }

        private void handleMCBEdit(PacketContainer<WrappedPlayInCustomPayload> packetContainer, ItemStack item, NBTCompound nbt) {
            ItemStack editedItem = handlePrerequisites(packetContainer, item, nbt, MaterialMappings.getTypeIfAny("writable-book"), (packetNBT, editedNBT) -> {
                editedNBT.put("pages", packetNBT.get("pages"));
            });
            if (editedItem == null) {
                return;
            }

            callHandleBookEditEvent(packetContainer.getPlayer().player, editedItem);
        }

        private void handleMCBSign(PacketContainer<WrappedPlayInCustomPayload> packetContainer, ItemStack item, NBTCompound nbt) {
            ItemStack editedItem = handlePrerequisites(packetContainer, item, nbt, Material.WRITTEN_BOOK, (packetNBT, editedNBT) -> {
                editedNBT.put("author", packetNBT.get("author"));
                editedNBT.put("title", packetNBT.get("title"));
                editedNBT.put("pages", packetNBT.get("pages"));
            });
            if (editedItem == null) {
                return;
            }

            callHandleBookEditEvent(packetContainer.getPlayer().player, editedItem);
        }

        private void callHandleBookEditEvent(Player player, ItemStack item) {
            IStructureModifier<Object> entityStructureModifier = ReflectStructureCache.acquireStructure(NMS.getCraftEntityClass())
                    .withType(NMS.getEntityClass())
                    .withTarget(player);
            Object entityPlayer = entityStructureModifier.readField(0);

            Object nmsItem = ItemReflection.getHandleItem(item);

            handleBookEditEventInvoker.invoke(null, entityPlayer, nmsItem);
        }

        private ItemStack handlePrerequisites(PacketContainer<WrappedPlayInCustomPayload> packetContainer,
                                              ItemStack item, NBTCompound nbt, Material type, BiConsumer<NBTCompound, NBTCompound> nbtConsumer) {

            ItemStack playerItem = getItemInMainHand(packetContainer.getPlayer().player);
            if (playerItem.getType() != MaterialMappings.getTypeIfAny("writable-book") || type != item.getType()) {
                return null;
            }

            playerItem.setType(type);

            NBTCompound editedNBT = NBTFactory.fromItem(playerItem, true);
            nbtConsumer.accept(nbt, editedNBT);

            return playerItem;
        }

        private ItemStack getItemInMainHand(Player player) {
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                return player.getInventory().getItemInHand();
            }
            else {
                return player.getInventory().getItemInMainHand();
            }
        }

        private boolean isValidBookTag(NBTCompound nbt) {
            String title;
            String author;
            return isValidQuillTag(nbt)
                    && nbt.hasKeyOfType("title", NBTType.STRING)
                    && (title = nbt.getString("title")) != null && title.length() <= ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.length() <= ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH;
        }

        private boolean isValidQuillTag(NBTCompound nbt) {
            return nbt != null
                    && nbt.hasKeyOfType("pages", NBTType.LIST)
                    && !isPageTooLong(nbt);
        }

        private boolean isPageTooLong(NBTCompound nbt) {
            for (NBTElement<String> page : nbt.<String>getListOf("pages", NBTType.STRING)) {
                if (page == null
                        || page.getValue().length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }
            return false;
        }
    }
}
