package me.alek.honey.netty.packet;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WritableBookContent;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Filterable;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemFactory;
import me.alek.honey.validation.item.ItemAnalysis;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PacketExecutors {

    public static void register() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPacketExecutor(PacketTypes.Play.Client.CUSTOM_PAYLOAD, new CustomPayloadPacketExecutor<>());
        }
    }

    private static class CustomPayloadPacketExecutor<CP extends WrappedPlayInCustomPayload<CP>> implements PacketExecutor<CP> {

        private static final Function<HoneyPlayer, Runnable> INVALID_BOOK_DATA = player -> () -> player.kick("Invalid book data!");

        @Override
        public boolean test(PacketContainer<CP> packet) {
            return packet.getAnyWrapped().getPayloadData().isBookChannel();
        }

        @Override
        public Runnable getTask(PacketContainer<CP> packet) {
            CP source = packet.getAnyWrapped();
            WrappedImmutableItemStack item = source.getPayloadData().getBookChannelItemStack();
            if (item == null) {
                source.release();
                return null;
            }

            Runnable edit = handleBookChannel(packet, item);
            return Optional.ofNullable(edit)
                    .map(r -> (Runnable) () -> {
                        source.release();
                        r.run();
                    })
                    .orElse(null);
        }

        private Runnable handleBookChannel(PacketContainer<CP> packet, WrappedImmutableItemStack item) {
            ItemStack heldItem = packet.getPlayer().getItemInMainHand();
            if (heldItem == null) {
                return INVALID_BOOK_DATA.apply(packet.getPlayer());
            }

            if (heldItem.getType() != ItemFactory.getMaterial(ItemTypes.WRITABLE_BOOK)) {
                return null;
            }

            if (ItemAnalysis.checkItem(packet.getPlayer(), item, null)) {
                return INVALID_BOOK_DATA.apply(packet.getPlayer());
            }

            String channel = packet.getCurrentWrapped().getPayloadData().getChannel();
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
                };
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();
                ArrayTag<String> pages = nbt.getArrayOf("pages", TagType.STRING);
                return () -> {
                    addPagesToWritableBook(heldItem, pages);
                    player.setItemInMainHand(heldItem);
                };
            } else {
                return null;
            }
        }

        private void addPagesToWritableBook(ItemStack item, Iterable<String> pages) {
            CompoundTag tag = NBTFactory.fromItem(item, true);
            tag.putArray("pages", TagType.STRING, pages);
        }

        private Runnable handleBookSign0(HoneyPlayer player, WrappedImmutableItemStack item, ItemStack heldItem) {
            if (item.getType() != ItemTypes.WRITTEN_BOOK && item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WrittenBookContent content = map.get(ComponentTypes.WRITTEN_BOOK_CONTENT).get();

                List<String> pages = content.getPages().stream()
                        .map(Filterable::getRaw)
                        .map(AdventureSerializer::toJson)
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

            try {
                ItemStack newItem = ItemFactory.createLiteralItem(new ItemStack(ItemFactory.getMaterial(ItemTypes.WRITTEN_BOOK)));
                ItemFactory.setItemTag(newItem, heldItemTag);
                player.setItemInMainHand(newItem);
            } catch (Exception ex) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to handle book sign", ex);
            }
        }

        private CompoundTag addContentToWrittenBook(ItemStack item, Iterable<String> pages, String author, String title) {
            CompoundTag tag = NBTFactory.fromItem(item, true);

            tag.putArray("pages", TagType.STRING,
                    StreamSupport.stream(pages.spliterator(), false)
                            .map(p -> AdventureSerializer.toJson(Component.text(p)))
                            .collect(Collectors.toList()));
            tag.putPrimitive("author", TagType.STRING, author);
            tag.putPrimitive("title", TagType.STRING, title);

            return tag;
        }
    }
}
