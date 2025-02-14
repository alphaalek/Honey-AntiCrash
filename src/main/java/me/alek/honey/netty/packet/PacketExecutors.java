package me.alek.honey.netty.packet;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WritableBookContent;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Filterable;
import com.google.common.util.concurrent.Runnables;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.tag.ComponentHelper;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.PrimitiveArrayTag;
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
                Runnable edit = handleBookEdit0(packet.getPlayer(), item);
                if (edit != null) {
                    return edit;
                }
            } else if (channel.equals(WrappedPlayInCustomPayload.PayloadData.SIGN_BOOK_CHANNEL)) {
                Runnable sign = handleBookSign0(packet.getPlayer(), item);
                if (sign != null) {
                    return sign;
                }
            }

            return INVALID_BOOK_DATA.apply(packet.getPlayer());
        }

        private Runnable handleBookEdit0(HoneyPlayer player, WrappedImmutableItemStack item) {
            if (item.getType() != ItemTypes.WRITABLE_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WritableBookContent content = map.get(ComponentTypes.WRITABLE_BOOK_CONTENT).get();
                return () -> applyContent(player, player.getItemInMainHand(), content.getPages().stream().map(Filterable::getRaw).collect(Collectors.toList()));
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();
                PrimitiveArrayTag<String> pages = nbt.getArrayOf("pages", TagType.STRING);
                return () -> applyContent(player, player.getItemInMainHand(), pages);
            } else {
                return null;
            }
        }

        private void applyContent(HoneyPlayer player, ItemStack heldItem, Iterable<String> pages) {
            CompoundTag heldItemTag = ItemFactory.getOrCreateTag(heldItem);
            heldItemTag.putArray("pages", TagType.STRING, pages);

            try {
                ItemStack newItem = ItemFactory.createLiteralItem(new ItemStack(ItemFactory.getMaterial(ItemTypes.WRITABLE_BOOK)));
                ItemFactory.setItemTag(newItem, heldItemTag);
                player.setItemInMainHand(newItem);
            } catch (Exception ex) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to handle book edit", ex);
            }
        }

        private Runnable handleBookSign0(HoneyPlayer player, WrappedImmutableItemStack item) {
            if (item.getType() != ItemTypes.WRITABLE_BOOK && item.getType() != ItemTypes.WRITTEN_BOOK) {
                return null;
            }

            ItemTag tag = item.getTag();
            if (tag.asComponentMap().isPresent()) {
                ComponentMap map = tag.asComponentMap().get();
                WrittenBookContent content = map.get(ComponentTypes.WRITTEN_BOOK_CONTENT).get();

                List<String> pages = content.getPages().stream()
                        .map(Filterable::getRaw)
                        .map(c -> (TextComponent) c) // forced to text component in tag-rewriter
                        .map(ComponentHelper::serialize)
                        .collect(Collectors.toList());
                String author = content.getAuthor();
                String title = content.getTitle().getRaw();

                return () -> applyContent(player, player.getItemInMainHand(), pages, author, title);
            } else if (tag.asNBT().isPresent()) {
                CompoundTag nbt = tag.asNBT().get();

                List<String> pages = nbt.<String>getPrimitiveArrayOf("pages", TagType.STRING).stream()
                        .collect(Collectors.toList());
                String author = nbt.getString("author");
                String title = nbt.getString("title");

                return () -> applyContent(player, player.getItemInMainHand(), pages, author, title);
            } else {
                return null;
            }
        }

        private void applyContent(HoneyPlayer player, ItemStack heldItem, Iterable<String> pages, String author, String title) {
            CompoundTag heldItemTag = ItemFactory.getOrCreateTag(heldItem);
            heldItemTag.putArray("pages", TagType.STRING, pages);
            heldItemTag.putPrimitive("author", TagType.STRING, author);
            heldItemTag.putPrimitive("title", TagType.STRING, title);

            try {
                ItemStack newItem = ItemFactory.createLiteralItem(new ItemStack(ItemFactory.getMaterial(ItemTypes.WRITTEN_BOOK)));
                ItemFactory.setItemTag(newItem, heldItemTag);
                player.setItemInMainHand(newItem);
            } catch (Exception ex) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to handle book sign", ex);
            }
        }
    }
}
