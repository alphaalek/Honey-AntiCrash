package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInTabComplete;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutTabComplete;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class TabCompleteListener extends AbstractPacketListener<WrappedPlayInTabComplete> implements IPacketListener {


    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInTabComplete> packetContainer, CheckContextContainer container) {
        return packetContainer.getPacket().getMessage().startsWith("/");
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInTabComplete> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInTabComplete> getVisitor() {
        CheckVisitor<WrappedPlayInTabComplete> visitor = new CheckVisitor<>();
        visitor.visit(new TabA());
        visitor.visit(new TabB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public void cleanup(Player player) {
        TabB.tabCompleteQueueMap.remove(player.getUniqueId());
    }

    public static class TabA implements PacketCheck<WrappedPlayInTabComplete>, ViolationCheck {

        private final String[] prohibitedCommands = ChatListener.getStatisfiedList(ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS);

        @Override
        public ResponseContainer check(Player player, WrappedPlayInTabComplete packet, CheckContextContainer container) {
            String message = packet.getMessage();
            String label = message.split(" ")[0];

            for (String prohibitedCommand : prohibitedCommands) {

                if (ChatListener.match(prohibitedCommand, label)) {
                    String messageFormatted = message.length() > 25 ? message.substring(0, 25) + "[...]" : message;
                    container.getDevelopmentData(this).addPoint("Message", messageFormatted);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Tab.A";
        }

        @Override
        public String getDescription() {
            return "Tab completing prohibited command";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Commands.class);
        }
    }

    public static class TabB implements PacketCheck<WrappedPlayInTabComplete>, ViolationCheck {

        private static final ViolationService.CheckKey key = new ViolationService.CheckKey(TabB.class, "TabB");
        private static final Map<UUID, LinkedBlockingQueue<ResponseListener>> tabCompleteQueueMap = new HashMap<>();

        private interface ResponseListener {

            void accept(int size);
        }

        public TabB() {
            HoneyPlugin.get().getPacketLibrary().addListener(new SyncPacketAdapter<WrappedPlayOutTabComplete>() {

                @Override
                public void onPacketSend(Player player, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {

                    synchronized (tabCompleteQueueMap) {
                        int size = packetContainer.getPacket().getSuggestionsSize();

                        try {
                            ResponseListener listener = tabCompleteQueueMap.computeIfAbsent(player.getUniqueId(), (d) -> new LinkedBlockingQueue<>()).poll();

                            if (listener != null) listener.accept(size);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }, ListenerPriority.NORMAL, () -> PacketType.Play.Server.TAB_COMPLETE);
        }

        @Override
        public ResponseContainer check(Player player, WrappedPlayInTabComplete packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                UUID uuid = player.getUniqueId();
                synchronized (tabCompleteQueueMap) {
                    tabCompleteQueueMap.computeIfAbsent(uuid, (d) -> new LinkedBlockingQueue<>()).add(new ResponseListener() {
                        @Override
                        public void accept(int size) {

                            container.getDevelopmentData(TabB.this).addPoint("Suggestions Size", size);
                            HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), key, null, size, getCheckName(), getSetting(), container.getDevelopmentData(TabB.this), false);
                        }
                    });
                }
                // force the buffer throttle to check if the buffer has exceeded the threshold by previous response listeners,
                // and therefore will block this packet. I do this by just flagging with 0.
                return ResponseBuilder.inst().flag(0).withLimitsKey(key).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() { return "Tab.B"; }

        @Override
        public String getDescription() {
            return "Buffer of tab complete suggestions exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.TabComplete.TabB.class);
        }
    }
}
