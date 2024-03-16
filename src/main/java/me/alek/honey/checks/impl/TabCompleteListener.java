package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInTabComplete;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutTabComplete;
import me.alek.honey.services.ViolationService;

import java.util.*;
import java.util.function.Supplier;

public class TabCompleteListener extends AbstractPacketListener<WrappedPlayInTabComplete> implements IPacketListener {


    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInTabComplete> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return packetContainer.getPacket().getMessage().startsWith("/");
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInTabComplete> packetContainer, CheckContextContainer container) {

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
        visitor.visit(new TabC());
        return visitor;
    }

    @Override
    public void load() {
        TabA.prohibitedCommands = ChatListener.getSatisfiedList(ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS);;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public void cleanup(HoneyPlayer player) {
        TabB.tabCompleteQueueMap.remove(player.uuid);
    }

    public static class TabA implements PacketCheck<WrappedPlayInTabComplete>, ViolationCheck {

        private static String[] prohibitedCommands;

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInTabComplete packet, CheckContextContainer container) {
            String message = packet.getMessage();
            String label = message.split(" ")[0];

            for (String prohibitedCommand : prohibitedCommands) {

                if (ChatListener.match(prohibitedCommand, label)) {
                    return ResponseBuilder.inst().cancel().flag()
                            .onViolation(() -> {
                                String messageFormatted = message.length() > 25 ? message.substring(0, 25) + "[...]" : message;;
                                container.getDevelopmentData(this).addPoint("Message", messageFormatted);
                            })
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 54;
        }

        @Override
        public String getName() {
            return "Tab.A";
        }

        @Override
        public String getDescription() {
            return "Tab completing prohibited command";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SINGLETON_TAB_COMPLETE_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabA.class);
        }
    }

    public static class TabB implements PacketCheck<WrappedPlayInTabComplete>, ViolationCheck {

        private static final ViolationService.CheckKey key = new ViolationService.CheckKey(TabB.class, "TabB");
        private static final Map<UUID, Queue<ResponseListener>> tabCompleteQueueMap = new HashMap<>();

        private interface ResponseListener {

            void accept(int size);
        }

        public TabB() {
            HoneyPlugin.get().getPacketLibrary().addListener(new SyncPacketAdapter<WrappedPlayOutTabComplete>() {

                @Override
                public void onPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
                    int size = packetContainer.getPacket().getSuggestionsSize();

                    ResponseListener listener = tabCompleteQueueMap.computeIfAbsent(player.uuid, (d) -> new LinkedList<>())
                            .poll();

                    if (listener != null) listener.accept(size);
                }

            }, ListenerPriority.NORMAL, () -> PacketType.Play.Server.TAB_COMPLETE);
        }

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInTabComplete packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                tabCompleteQueueMap.computeIfAbsent(player.uuid, (d) -> new LinkedList<>()).add(new ResponseListener() {
                    @Override
                    public void accept(int size) {
                        container.getDevelopmentData(TabB.this).addPoint("Suggestions Size", size);
                        HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), key, null, size, TabB.this, getSetting(), container.getDevelopmentData(TabB.this));
                    }
                });
                // force the buffer throttle to check if the buffer has exceeded the threshold by previous response listeners,
                // and therefore will block this packet. I do this by just flagging with 0.
                return ResponseBuilder.inst().flag(0).withLimitsKey(key).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 55;
        }

        @Override
        public String getName() {
            return "Tab.B";
        }

        @Override
        public String getDescription() {
            return "Buffer of tab complete suggestions exceeded";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_TAB_COMPLETE_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabB.class);
        }
    }

    public static class TabC implements PacketCheck<WrappedPlayInTabComplete>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInTabComplete packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(packet.getMessage().length()).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 56;
        }

        @Override
        public String getName() {
            return "Tab.C";
        }

        @Override
        public String getDescription() {
            return "Length of tab complete exceeded";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_TAB_COMPLETE_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabC.class);
        }
    }
}
