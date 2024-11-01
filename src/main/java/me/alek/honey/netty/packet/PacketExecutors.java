package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTElement;
import me.alek.honey.netty.nbt.NBTType;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.*;
import me.alek.honey.validation.item.ItemAnalysis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketExecutors {

    public static void register() {
        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            packetProcessor.addPacketExecutor(PacketTypes.Play.Client.CUSTOM_PAYLOAD, new CustomPayloadPacketExecutor<>());
        }
    }

    private static class CustomPayloadPacketExecutor<CP extends WrappedPlayInCustomPayload<CP>> implements PacketExecutor<CP> {

        private static final Function<Player, Runnable> INVALID_BOOK_DATA = p -> () -> p.kickPlayer("Invalid book data!");

        private static final ConstructorInvoker<Object> chatComponentTextConstructor = Reflection.getConstructor(
                Reflection.getClass("{nms}.ChatComponentText"), String.class);
        private static final MethodInvoker<Object> chatComponentSerializerInvoker = Reflection.getMethod(
                NMS.getChatSerializerClass(), 0, NMS.getIChatBaseComponentClass());

        private static final MethodInvoker<Object> stripEventsInvoker = Reflection.getMethod(
                Reflection.getClass("{obc}.event.CraftEventFactory"), "stripEvents", NMS.getIChatBaseComponentClass());
        private static final ConstructorInvoker<BookMeta> bookMetaConstructor = Reflection.getConstructor(
                Reflection.getClass("{obc}.inventory.CraftMetaBook"), NMS.getNBTTagCompoundClass()
        );
        private static final ConstructorInvoker<BookMeta> bookSignedMetaConstructor = Reflection.getConstructor(
                Reflection.getClass("{obc}.inventory.CraftMetaBookSigned"), NMS.getNBTTagCompoundClass()
        );

        @Override
        public boolean test(PacketContainer<CP> packetContainer) {
            return packetContainer.getAnyWrapped().getData().isBookChannel();
        }

        @Override
        public Runnable getTask(PacketContainer<CP> packetContainer) {
            WrappedImmutableItemStack item = packetContainer.getAnyWrapped().getData().getBookChannelItemStack();
            if (item == null || item.getNBT() == null) {
                // can't edit or sign book, if item or nbt is invalid
                packetContainer.getAnyWrapped().release();
                return null;
            }

            Runnable edit = handleBookChannel(packetContainer, item);
            return Optional.ofNullable(edit)
                .map(r -> (Runnable) () -> {
                    packetContainer.getAnyWrapped().release();
                    r.run();
                })
                .orElse(null);
        }

        private Runnable handleBookEdit(Player player, WrappedImmutableItemStack item, ItemStack heldItem, BookMeta newBookMeta) {
            if (item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return null;
            }

            return () -> handleItem(player, item.getNBT(), false, (oldMeta, newMeta) -> {
                heldItem.setItemMeta(newMeta);
                setItemInMainHand(player, heldItem);
            });
        }

        private Runnable handleBookSign(Player player, PacketContainer<CP> packet, WrappedImmutableItemStack item, ItemStack heldItem, BookMeta newBookMeta) {
            if (!isValidAuthor(packet.getPlayer(), item.getNBT())) {
                return null;
            }

            if (item.getType() != Material.WRITTEN_BOOK
                    && item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return null;
            }

            if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
                NBTArray<String> pages = item.getNBT().hasKeyOfType("pages", NBTType.STRING)
                        ? item.getNBT().getListOf("pages", NBTType.STRING)
                        : new NBTArray<>(NBTType.STRING);
                if (!serializeTextComponents(pages)) {
                    // TODO: actually serialize the components of the meta
                    return null;
                }
            }
            stripEvents(newBookMeta);

            return () -> handleItem(player, item.getNBT(), true, (oldMeta, newMeta) -> {
                heldItem.setType(Material.WRITTEN_BOOK);
                heldItem.setItemMeta(newMeta);
                setItemInMainHand(player, heldItem);
            });
        }

        private Runnable handleBookChannel(PacketContainer<CP> packet, WrappedImmutableItemStack item) {
            Player bukkitPlayer = packet.getPlayer().bukkit;
            ItemStack heldItem = getItemInMainHand(bukkitPlayer);
            if (heldItem == null) {
                return null;
            }

            if (ItemAnalysis.checkItem(packet.getPlayer(), item, null)) {
                return INVALID_BOOK_DATA.apply(bukkitPlayer);
            }

            String channel = packet.getAnyWrapped().getData().getChannel();

            if (channel.equals("MC|BEdit")) {
                BookMeta newItemMeta = bookSignedMetaConstructor.invoke(item.getNBT().getHandle());
                Runnable edit = handleBookEdit(bukkitPlayer, item, heldItem, newItemMeta);
                if (edit != null) {
                    return edit;
                }
            } else if (channel.equals("MC|BSign")) {
                BookMeta newItemMeta = bookMetaConstructor.invoke(item.getNBT().getHandle());
                Runnable sign = handleBookSign(bukkitPlayer, packet, item, heldItem, newItemMeta);
                if (sign != null) {
                    return sign;
                }
            }

            return INVALID_BOOK_DATA.apply(bukkitPlayer);
        }

        private void handleItem(Player player, NBTCompound nbt, boolean signing, BiConsumer<ItemMeta, ItemMeta> success) {
            ItemStack heldItem = getItemInMainHand(player);
            if (heldItem.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                // can't edit or sign book, if the held item of the player is not writable book type
                return;
            }

            BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();

            BookMeta newItemMeta;
            if (signing) {
                newItemMeta = bookSignedMetaConstructor.invoke(nbt.getHandle());
            } else {
                newItemMeta = bookMetaConstructor.invoke(nbt.getHandle());
            }
            PlayerEditBookEvent editBookEvent = new PlayerEditBookEvent(
                    player, player.getInventory().getHeldItemSlot(), heldItemMeta, newItemMeta, signing
            );
            Bukkit.getPluginManager().callEvent(editBookEvent);
            if (editBookEvent.isCancelled()) {
                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), heldItem);
                return;
            }

            success.accept(heldItemMeta, newItemMeta);
        }

        private void stripEvents(Object meta) {
            List<Object> pages = ItemReflection.getPages((BookMeta) meta);

            List<Object> newPages = new ArrayList<>();
            for (Object page : pages) {
                newPages.add(stripEventsInvoker.invoke(null, page));
            }

            ItemReflection.setPages((BookMeta) meta, newPages);
        }

        private boolean serializeTextComponents(NBTArray<String> pages) {
            for (int i = 0; i < pages.size(); i++) {
                NBTElement<String> page = pages.get(i);

                try {
                    Object chatComponent = chatComponentTextConstructor.invoke(page.getValue());
                    String serialized = (String) chatComponentSerializerInvoker.invoke(null, chatComponent);

                    pages.set(i, new NBTElement<>(NBTType.STRING, serialized));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            return true;
        }

        private ItemStack getItemInMainHand(Player player) {
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                return player.getInventory().getItemInHand();
            } else {
                return player.getInventory().getItemInMainHand();
            }
        }

        private void setItemInMainHand(Player player, ItemStack item) {
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                player.getInventory().setItemInHand(item);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
        }

        private boolean isValidAuthor(HoneyPlayer player, NBTCompound nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.equals(player.name);
        }
    }
}
