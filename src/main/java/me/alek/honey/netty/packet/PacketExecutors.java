package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTElement;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTType;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTList;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.util.*;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class PacketExecutors {

    public static void register() {
        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            packetProcessor.addPacketExecutor(PacketTypes.Play.Client.CUSTOM_PAYLOAD, new CustomPayloadPacketExecutor());
        }
    }

    private static class CustomPayloadPacketExecutor implements PacketExecutor<WrappedPlayInCustomPayload> {

        private static final Class<?> craftEventFactory = Reflection.getClass("{obc}.event.CraftEventFactory");

        private static final ConstructorInvoker<Object> chatComponentTextConstructor = Reflection.getConstructor(
                Reflection.getClass("{nms}.ChatComponentText"), String.class);
        private static final MethodInvoker<Object> chatComponentSerializerInvoker = Reflection.getMethod(
                Reflection.getClass("{nms}.IChatBaseComponent$ChatSerializer"), 0, Reflection.getClass("{nms}.IChatBaseComponent"));

        private static final MethodInvoker<Object> stripEventsInvoker = Reflection.getMethod(
                craftEventFactory, "stripEvents", NMS.getIChatBaseComponentClass());
        private static final AtomicReference<FieldAccessor<List<Object>>> bookMetaPageCache = new AtomicReference<>();

        @Override
        public boolean test(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            String channel = packetContainer.getPacket().getChannel();

            return channel.equals("MC|BEdit") || channel.equals("MC|BSign");
        }

        @Override
        public Runnable getTask(PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
            ItemStack item = packetContainer.getPacket().getItemStack();
            if (item == null) {
                return null;
            }

            NBTCompound nbt = NBTFactory.fromItem(item, false, false);
            return handleBookEdit(packetContainer, item, nbt);
        }

        private Runnable handleBookEdit(PacketContainer<WrappedPlayInCustomPayload> packetContainer, ItemStack item, NBTCompound nbt) {
            Player player = packetContainer.getPlayer().player;
            ItemMeta meta = item.getItemMeta();
            switch (packetContainer.getPacket().getChannel()) {
                case "MC|BEdit":
                    if (ItemAnalysis.checkItem(packetContainer.getPlayer(), item, nbt, null)) {
                        break;
                    }

                    if (item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                        break;
                    }

                    stripEvents(meta);

                    return () -> handleItem(player, (BookMeta) meta, (i, m) -> {
                        i.setItemMeta((ItemMeta) ReflectStructure.copyStructure(meta, m));
                        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), i);
                    });
                case "MC|BSign":
                    if (ItemAnalysis.checkItem(packetContainer.getPlayer(), item, nbt, null) ||
                            !isValidAuthor(packetContainer.getPlayer(), nbt)) {
                        break;
                    }

                    if (item.getType() != Material.WRITTEN_BOOK
                            && item.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                        break;
                    }

                    if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
                        NBTList<String> pages = nbt.hasKeyOfType("pages", NBTType.STRING)
                                ? nbt.getListOf("pages", NBTType.STRING)
                                : new NBTList<>(NBTType.STRING);
                        if (!serializeTextComponents(pages)) {
                            break;
                        }
                    }
                    stripEvents(meta);

                    return () -> handleItem(player, (BookMeta) meta, (i, m) -> {
                        i.setType(Material.WRITTEN_BOOK);
                        i.setItemMeta((ItemMeta) ReflectStructure.copyStructure(meta, m));
                    });
                default:
                    return null;
            }
            return () -> player.kickPlayer("Invalid book data!");
        }

        private void handleItem(Player player, BookMeta meta, BiConsumer<ItemStack, ItemMeta> success) {
            ItemStack heldItem = getItemInMainHand(player);
            if (heldItem.getType() != MaterialMappings.getTypeIfAny("writable-book")) {
                return;
            }

            BookMeta heldItemMeta = (BookMeta) heldItem.getItemMeta();
            PlayerEditBookEvent editBookEvent = new PlayerEditBookEvent(
                    player, player.getInventory().getHeldItemSlot(), heldItemMeta, meta, true
            );
            Bukkit.getPluginManager().callEvent(editBookEvent);
            if (editBookEvent.isCancelled()) {
                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), heldItem);
                return;
            }

            success.accept(heldItem, heldItemMeta);
        }

        private void stripEvents(Object meta) {
            AtomicReference<IStructureModifier<List<Object>>> bookMetaPageModifier = new AtomicReference<>();
            List<Object> pages = ItemReflection.getPages(bookMetaPageModifier, (BookMeta) meta);

            List<Object> newPages = new ArrayList<>();
            for (Object page : pages) {
                newPages.add(stripEventsInvoker.invoke(null, page));
            }

            bookMetaPageModifier.get().writeField(bookMetaPageCache, "pages", 0, newPages);
        }

        private boolean serializeTextComponents(NBTList<String> pages) {
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

        private boolean isValidAuthor(HoneyPlayer player, NBTCompound nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.equals(player.name);
        }
    }
}
