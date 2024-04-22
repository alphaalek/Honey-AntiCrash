package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTElement;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTType;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.*;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PacketExecutors {

    public static void register() {
        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            packetProcessor.addPacketExecutor(new CustomPayloadPacketExecutor(), PacketType.Play.Client.CUSTOM_PAYLOAD);
        }
    }

    private static class CustomPayloadPacketExecutor implements PacketExecutor<WrappedPlayInCustomPayload> {

        private static final Class<?> craftEventFactory = Reflection.getClass("{obc}.event.CraftEventFactory");
        private static final MethodInvoker<Object> handleBookEditEventInvoker = Reflection.getMethod(craftEventFactory, "handleEditBookEvent", NMS.getEntityPlayerClass(), NMS.getItemStackClass());
        private static final ConstructorInvoker<Object> chatComponentTextConstructor = Reflection.getConstructor(
                Reflection.getClass("{nms}.ChatComponentText"), String.class);
        private static final MethodInvoker<Object> chatComponentSerializerInvoker = Reflection.getMethod(
                Reflection.getClass("{nms}.IChatBaseComponent$ChatSerializer"), 0, Reflection.getClass("{nms}.IChatBaseComponent"));

        @Override
        public boolean accepts(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            String channel = packetContainer.getPacket().getChannel();

            return channel.equals("MC|BEdit") || channel.equals("MC|BSign");
        }

        @Override
        public Runnable getTask(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            WrappedPacketDataSerializer serializer = new WrappedPacketDataSerializer(packetContainer.getPacket().getDataSerializer().getHandle());
            ItemStack item = null;
            try {
                item = Converters.getItemStackConverter().convertDelegate(serializer.readItemStack());
            } catch (Exception ex) {

            }
            serializer.release();

            if (item == null) {
                return null;
            }

            NBTCompound nbt = NBTFactory.fromItem(item, false, false);

            switch (packetContainer.getPacket().getChannel()) {
                case "MC|BEdit":
                    if (!isValidQuillTag(nbt)) {
                        break;
                    }

                    return () -> handleMCBEdit(packetContainer, nbt);
                case "MC|BSign":
                    if (!isValidBookTag(nbt) || !isValidAuthor(nbt, packetContainer.getPlayer())) {
                        break;
                    }

                    NBTList<String> pages = nbt.getListOf("pages", NBTType.STRING);
                    if (pages == null) {
                        pages = new NBTList<>(NBTType.STRING);
                    }

                    if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
                        serializeTextComponents(pages);
                    }

                    NBTList<String> finalPages = pages;
                    return () -> handleMCBSign(packetContainer, nbt, finalPages);
                default:
                    return null;
            }
            return () -> packetContainer.getPlayer().player.kickPlayer("Invalid book data!");
        }

        private void handleMCBEdit(PacketContainer<WrappedPlayInCustomPayload> packetContainer, NBTCompound nbt) {
            ItemStack editedItem = handlePrerequisites(packetContainer, MaterialMappings.getTypeIfAny("writable-book"), editedNBT -> {
                editedNBT.put("pages", nbt.get("pages"));
            });

            if (editedItem == null) {
                return;
            }

            callHandleBookEditEvent(packetContainer.getPlayer().player, editedItem);
        }

        private void handleMCBSign(PacketContainer<WrappedPlayInCustomPayload> packetContainer, NBTCompound nbt, NBTList<String> pages) {
            ItemStack editedItem = handlePrerequisites(packetContainer, Material.WRITTEN_BOOK, editedNBT -> {
                editedNBT.put("author", nbt.get("author"));
                editedNBT.put("title", nbt.get("title"));
                editedNBT.put("pages", pages);
            });

            if (editedItem == null) {
                return;
            }

            callHandleBookEditEvent(packetContainer.getPlayer().player, editedItem);
        }

        private void serializeTextComponents(NBTList<String> pages) {
            for (int i = 0; i < pages.size(); i++) {
                NBTElement<String> page = pages.get(i);

                try {
                    Object chatComponent = chatComponentTextConstructor.invoke(page.getValue());
                    String serialized = (String) chatComponentSerializerInvoker.invoke(null, chatComponent);

                    pages.set(i, new NBTElement<>(NBTType.STRING, serialized));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
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
                                              Material type, Consumer<NBTCompound> nbtConsumer) {

            ItemStack playerItem = getItemInMainHand(packetContainer.getPlayer().player);
            if (playerItem.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return null;
            }

            if (playerItem.getType() != type) {
                playerItem.setType(type);
            }

            NBTCompound editedNBT = NBTFactory.fromItem(playerItem, true, false);
            nbtConsumer.accept(editedNBT);

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

        private boolean isValidAuthor(NBTCompound nbt, HoneyPlayer player) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.equals(player.name);
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
