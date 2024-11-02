package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.structure.ReflectStructure;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        private Runnable handleBookEdit(Player player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return null;
            }

            return () -> handleItem(player, item, false, (oldMeta, newMeta) -> {
                BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();
                heldItemMeta.setPages(newMeta.getPages());
                heldItem.setItemMeta(heldItemMeta);

                setItemInMainHand(player, heldItem);
            });
        }

        private Runnable handleBookSign(Player player, PacketContainer<CP> packet, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (!isValidAuthor(packet.getPlayer(), item.getNBT())) {
                return null;
            }

            if (item.getType() != Material.WRITTEN_BOOK
                    && item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return null;
            }

            if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
                ArrayTag<String> pages = item.getNBT().hasKeyOfType("pages", TagType.STRING)
                        ? item.getNBT().getArrayOf("pages", TagType.STRING)
                        : new ArrayTag<>(TagType.STRING);
                if (!serializeTextComponents(pages)) {
                    // TODO: actually serialize the components of the meta
                    return null;
                }
            }

            return () -> handleItem(player, item, true, (oldMeta, newMeta) -> {
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
                Runnable edit = handleBookEdit(bukkitPlayer, item, heldItem);
                if (edit != null) {
                    return edit;
                }
            } else if (channel.equals("MC|BSign")) {
                Runnable sign = handleBookSign(bukkitPlayer, packet, item, heldItem);
                if (sign != null) {
                    return sign;
                }
            }

            return INVALID_BOOK_DATA.apply(bukkitPlayer);
        }

        private void handleItem(Player player, WrappedImmutableItemStack item, boolean signing, BiConsumer<BookMeta, BookMeta> successCallback) {
            ItemStack heldItem = getItemInMainHand(player);
            if (heldItem.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                // can't edit or sign book, if the held item of the player is not writable book type
                return;
            }

            BookMeta newItemMeta = (BookMeta) item.getSpecificMeta();
            BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();
            stripEvents(newItemMeta);

            if (PlayerEditBookEvent.getHandlerList().getRegisteredListeners().length > 0) {
                PlayerEditBookEvent editBookEvent = new PlayerEditBookEvent(
                        player, player.getInventory().getHeldItemSlot(), heldItemMeta, newItemMeta, signing
                );
                Bukkit.getPluginManager().callEvent(editBookEvent);
                if (editBookEvent.isCancelled()) {
                    setItemInMainHand(player, heldItem); // update for client
                    return;
                }
            }

            successCallback.accept(heldItemMeta, newItemMeta);
        }

        private void stripEvents(BookMeta meta) {
            List<Object> pages = ItemReflection.getPages(meta);

            List<Object> newPages = new ArrayList<>();
            for (Object page : pages) {
                newPages.add(stripEventsInvoker.invoke(null, page));
            }

            ItemReflection.setPages(meta, newPages);
        }

        private boolean serializeTextComponents(ArrayTag<String> pages) {
            for (int i = 0; i < pages.size(); i++) {
                PrimitiveTag<String> page = pages.get(i);

                try {
                    Object chatComponent = chatComponentTextConstructor.invoke(page.getValue());
                    String serialized = (String) chatComponentSerializerInvoker.invoke(null, chatComponent);

                    pages.set(i, new PrimitiveTag<>(TagType.STRING, serialized));
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

        private boolean isValidAuthor(HoneyPlayer player, CompoundTag nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", TagType.STRING)
                    && (author = nbt.getString("author")) != null && author.equals(player.name);
        }
    }
}
