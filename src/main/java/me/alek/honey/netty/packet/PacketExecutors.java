package me.alek.honey.netty.packet;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WritableBookContent;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Filterable;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.google.common.util.concurrent.Runnables;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
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
import net.kyori.adventure.text.TextComponent;
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
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPacketExecutor(PacketTypes.Play.Client.CUSTOM_PAYLOAD, new CustomPayloadPacketExecutor());
        }
    }

    private static class CustomPayloadPacketExecutor implements PacketExecutor<WrappedPlayInCustomPayload> {

        private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();
        private static final Function<HoneyPlayer, Runnable> INVALID_BOOK_DATA = player -> () -> player.kick("Invalid book data!");

        @Override
        public boolean test(PacketContainer<WrappedPlayInCustomPayload> packet) {
            return packet.getAnyWrapped().getPayloadData().isBookChannel();
        }

        @Override
        public Runnable getTask(PacketContainer<WrappedPlayInCustomPayload> packet) {
            WrappedPlayInCustomPayload source = packet.getAnyWrapped();
            WrappedImmutableItemStack item = source.getPayloadData().getBookChannelItemStack();
            if (item == null) {
                return null;
            }

            Runnable task = handleBookChannel(packet, item);
            return Optional.ofNullable(task).orElse(Runnables.doNothing());
        }

        private Runnable handleBookChannel(PacketContainer<WrappedPlayInCustomPayload> packet, WrappedImmutableItemStack item) {
            ItemStack heldItem = packet.getPlayer().getItemInMainHand();
            if (heldItem == null || heldItem.getType() != ItemFactory.getMaterial(ItemTypes.WRITABLE_BOOK)) {
                return null;
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
            NetProtocol protocol = player.clientInfo.getProtocol();
            if (protocol.isOlderThanOrEqual(NetProtocol.v1_8_8) && item.getType() != ItemTypes.WRITTEN_BOOK) {
                return null;
            } else if (protocol.isNewerThan(NetProtocol.v1_8_8) && item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WrittenBookContent content = map.get(ComponentTypes.WRITTEN_BOOK_CONTENT).get();

                List<String> pages = content.getPages().stream()
                        .map(Filterable::getRaw)
                        .map(this::ensureTextComponent)
                        .map(GSON_COMPONENT_SERIALIZER::serialize)
                        .collect(Collectors.toList());
                String author = content.getAuthor();
                String title = content.getTitle().getRaw();

                return () -> applyContent(player, heldItem, pages, author, title);
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();

                List<String> pages = StreamSupport.stream(nbt.<String>getArrayOf("pages", TagType.STRING).spliterator(), false)
                        .map(GSON_COMPONENT_SERIALIZER::deserialize)
                        .map(this::ensureTextComponent)
                        .map(GSON_COMPONENT_SERIALIZER::serialize)
                        .collect(Collectors.toList());
                String author = nbt.getString("author");
                String title = nbt.getString("title");

                return () -> applyContent(player, heldItem, pages, author, title);
            } else {
                return null;
            }
        }

        private Component ensureTextComponent(Component component) {
            if (!(component instanceof TextComponent)) {
                return Component.text(GSON_COMPONENT_SERIALIZER.serialize(component));
            } else {
                return component;
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

            tag.putArray("pages", TagType.STRING, pages);
            tag.putPrimitive("author", TagType.STRING, author);
            tag.putPrimitive("title", TagType.STRING, title);

            return tag;
        }
    }
}
