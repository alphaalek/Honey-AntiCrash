package me.alek.honey.netty.packet;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WritableBookContent;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Filterable;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.text.Filtered;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.PrimitiveTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.*;
import me.alek.honey.validation.item.ItemAnalysis;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PacketExecutors {

    public static void register() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPacketExecutor(PacketTypes.Play.Client.CUSTOM_PAYLOAD, new CustomPayloadPacketExecutor<>());
        }
    }

    private static class CustomPayloadPacketExecutor<CP extends WrappedPlayInCustomPayload<CP>> implements PacketExecutor<CP> {

        private static final Function<HoneyPlayer, Runnable> INVALID_BOOK_DATA = player -> () -> player.kick(Component.text("Invalid book data!"));

        @Override
        public boolean test(PacketContainer<CP> packet) {
            return packet.getAnyWrapped().getPayloadData().isBookChannel();
        }

        @Override
        public Runnable getTask(PacketContainer<CP> packet) {
            WrappedImmutableItemStack item = packet.getAnyWrapped().getPayloadData().getBookChannelItemStack();
            if (item == null) {
                packet.getAnyWrapped().release();
                return null;
            }

            Runnable edit = handleBookChannel(packet, item);
            return Optional.ofNullable(edit)
                    .map(r -> (Runnable) () -> {
                        packet.getAnyWrapped().release();
                        r.run();
                    })
                    .orElse(null);
        }

        private Runnable handleBookChannel(PacketContainer<CP> packet, WrappedImmutableItemStack item) {
            ItemStack heldItem = packet.getPlayer().getItemInMainHand();
            if (heldItem == null) {
                return INVALID_BOOK_DATA.apply(packet.getPlayer());
            }

            if (ItemAnalysis.checkItem(packet.getPlayer(), item, null)) {
                return INVALID_BOOK_DATA.apply(packet.getPlayer());
            }

            String channel = packet.getAnyWrapped().getPayloadData().getChannel();
            if (channel.equals(WrappedPlayInCustomPayload.PayloadData.EDIT_BOOK_CHANNEL)) {
                Runnable edit = handleBookEdit0(packet.getPlayer(), item, heldItem);
                if (edit != null) {
                    return edit;
                }
            } else if (channel.equals(WrappedPlayInCustomPayload.PayloadData.SIGN_BOOK_CHANNEL)) {
                Runnable sign = handleBookSign0(packet.getPlayer(), item, heldItem);
                if (sign != null) {
                    return sign;
                }
            }

            return INVALID_BOOK_DATA.apply(packet.getPlayer());
        }

        private Runnable handleBookEdit0(HoneyPlayer player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WritableBookContent content = map.get(ComponentTypes.WRITABLE_BOOK_CONTENT).get();
                return () -> {
                    addPagesToWritableBook(heldItem, content.getPages().stream().map(Filterable::getRaw).collect(Collectors.toList()));
                    player.setItemInMainHand(heldItem);

                    Bukkit.broadcastMessage("Edit " + heldItem);
                };
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();
                ArrayTag<String> pages = nbt.getArrayOf("pages", TagType.STRING);
                return () -> {
                    addPagesToWritableBook(heldItem, pages);
                    player.setItemInMainHand(heldItem);

                    Bukkit.broadcastMessage("Edit " + heldItem);
                };
            } else {
                return null;
            }
        }

        private CompoundTag addPagesToWritableBook(ItemStack item, Iterable<String> pages) {
            CompoundTag tag = NBTFactory.fromItem(item, true);
            ArrayTag<String> pagesTag = new ArrayTag<>(TagType.STRING);
            pagesTag.addAll(pages);
            tag.put("pages", pagesTag);
            return tag;
        }

        private Runnable handleBookSign0(HoneyPlayer player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (item.getType() != ItemTypes.WRITTEN_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WrittenBookContent content = map.get(ComponentTypes.WRITTEN_BOOK_CONTENT).get();

                List<String> pages = content.getPages().stream()
                        .map(Filterable::getRaw)
                        .map(p -> AdventureSerializer.getGsonSerializer().serialize(p))
                        .collect(Collectors.toList());
                String author = content.getAuthor();
                String title = content.getTitle().getRaw();
                return () -> applyContent(player, heldItem, pages, author, title);
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();
                ArrayTag<String> pages = nbt.getArrayOf("pages", TagType.STRING);
                String author = nbt.getString("author");
                String title = nbt.getString("title");
                return () -> applyContent(player, heldItem, pages, author, title);
            } else {
                return null;
            }
        }

        private void applyContent(HoneyPlayer player, ItemStack heldItem, Iterable<String> pages, String author, String title) {
            CompoundTag heldItemTag = addContentToWrittenBook(heldItem, pages, author, title);

            ItemStack newItem = new ItemStack(ItemFactory.getMaterial(ItemTypes.WRITTEN_BOOK));
            ItemFactory.setItemTag(newItem, heldItemTag);
            player.setItemInMainHand(newItem);

            Bukkit.broadcastMessage("Sign " + newItem + " " + heldItemTag);
        }

        private CompoundTag addContentToWrittenBook(ItemStack item, Iterable<String> pages, String author, String title) {
            CompoundTag tag = NBTFactory.fromItem(item, true);
            Bukkit.broadcastMessage(StreamSupport.stream(pages.spliterator(), false)
                    .map(p -> AdventureSerializer.getLegacySerializer().serialize(Component.text(p)))
                    .collect(Collectors.toList()) + "");
            tag.put("pages", new ArrayTag<>(TagType.STRING,
                    StreamSupport.stream(pages.spliterator(), false)
                            .map(p -> AdventureSerializer.getLegacySerializer().serialize(Component.text(p)))
                            .collect(Collectors.toList())
            ));
            tag.put("author", new PrimitiveTag<>(TagType.STRING, author));
            tag.put("title", new PrimitiveTag<>(TagType.STRING, title));
            return tag;
        }

        /*
        private Runnable handleBookEdit(HoneyPlayer player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            return () -> handleItem(player, item, false, (oldMeta, newMeta) -> {
                BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();
                heldItemMeta.setPages(newMeta.getPages());
                heldItem.setItemMeta(heldItemMeta);

                player.setItemInMainHand(heldItem);

            });
        }

        private Runnable handleBookSign(HoneyPlayer player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (!isValidAuthor(player, item.getTag().asNBT().get())) {
                return null;
            }

            if (item.getType() != ItemTypes.WRITTEN_BOOK && item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            // can't figure out why this breaks the page to show the json representation instead of the literal page, when this below is
            // what is done in 1.9+ server code. But works fine when disabled, so I'm just accepting it I guess.

            if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
                ArrayTag<String> pages = item.getNBT().getArrayOf("pages", TagType.STRING);

                if (!serializeTextComponents(pages)) {
                    return null;
                }

                item.clearSpecificMetaCache();
            }


            return () -> handleItem(player, item, true, (oldMeta, newMeta) -> {
                heldItem.setType(Material.WRITTEN_BOOK);
                heldItem.setItemMeta(newMeta);
                player.setItemInMainHand(heldItem);
            });
        }

        private void handleItem(HoneyPlayer player, WrappedImmutableItemStack item, boolean signing, BiConsumer<BookMeta, BookMeta> successCallback) {
            ItemStack heldItem = player.getItemInMainHand();
            if (heldItem.getType() != MaterialMappings.getTypeOnly("writable-book")) {
                // can't edit or sign book, if the held item of the player is not writable book type
                return;
            }

            BookMeta newItemMeta = (BookMeta) item.getSpecificMeta();
            BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();
            stripComponentEvents(newItemMeta);

            if (PlayerEditBookEvent.getHandlerList().getRegisteredListeners().length > 0) {
                PlayerEditBookEvent editBookEvent = new PlayerEditBookEvent(
                        player.bukkit, player.bukkit.getInventory().getHeldItemSlot(), heldItemMeta, newItemMeta, signing
                );
                Bukkit.getPluginManager().callEvent(editBookEvent);
                if (editBookEvent.isCancelled()) {
                    player.setItemInMainHand(heldItem); // update for client
                    return;
                }
            }

            successCallback.accept(heldItemMeta, newItemMeta);
        }


        private void stripComponentEvents(BookMeta meta) {
            List<Object> pages = ItemFactory.getPages(meta);
            if (pages != null) {
                List<Object> newPages = new ArrayList<>();
                for (Object page : pages) {
                    newPages.add(stripEventsInvoker.invoke(null, page));
                }

                ItemFactory.setPages(meta, newPages);
            }
        }

        private boolean serializeTextComponents(ArrayTag<String> pages) {
            for (int i = 0; i < pages.size(); i++) {
                String page = pages.get(i);

                try {
                    Object chatComponent = chatComponentTextConstructor.invoke(page);
                    String serialized = (String) chatComponentSerializerInvoker.invoke(null, chatComponent);

                    pages.set(i, serialized);
                } catch (Exception ex) {
                    return false;
                }
            }

            return true;
        }
        */

        private boolean isValidAuthor(HoneyPlayer player, CompoundTag nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", TagType.STRING)
                    && (author = nbt.getString("author")) != null && author.equals(player.name);
        }
    }
}
