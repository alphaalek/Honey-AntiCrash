package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.WideTempQueuedHandleTracker;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends SyncPacketAdapter<WP> implements IPacketListener {

    private CheckVisitor<WP> cachedVisitor;
    private boolean initializedCheckInstances;

    public static void applyCommonDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
        developmentData.addValue(" Version", Bukkit.getServer().getVersion());
        developmentData.addEmpty();
        developmentData.addHeader("Description", null);

        // temporary placeholders
        developmentData.addEmpty();
        developmentData.addEmpty();
        developmentData.addEmpty();
    }

    public static void applyQuotients(Player player, CheckContextContainer.DevelopmentData developmentData) {
        WideTempQueuedHandleTracker serverThreadHandleTracker = HoneyPlugin.get().getServerThreadHandleTracker();
        WideTempQueuedHandleTracker nettyThreadHandleTracker = HoneyPlugin.get().getNettyPacketHandleTracker();

        String serverThreadPoint = formatPercentages(serverThreadHandleTracker.getStatistics(player));
        String nettyThreadPoint = formatPercentages(nettyThreadHandleTracker.getStatistics(player));

        developmentData.setPoint(6, "STQ", serverThreadPoint);
        developmentData.setPoint(7, "NTQ", nettyThreadPoint);
    }

    private static String formatPercentages(double[] percentages) {
        StringBuilder message = new StringBuilder();

        for (double percentage : percentages) {
            if (percentage == 0) percentage = 1;
            message.append(", ").append(NumberConversions.format(percentage * 100, 2)).append("%");
        }
        return message.substring(2, message.length());
    }

    public void onPacketReceive(Player player, PacketContainer<WP> packetContainer) throws Exception {
        if (packetContainer.getState() == PacketState.PLAY) {
            if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
                return;
            }
        }
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        if (player != null) {
            CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

            defaultDevelopmentData.addEmpty();

            String packet = packetContainer.getHandle().getClass().getSimpleName();
            defaultDevelopmentData.addValue(" Packet", packet);
            applyCommonDevelopmentData(defaultDevelopmentData);
        }

        if (!onPrePacketReceive(player, packetContainer, checkContextContainer)) {
            return;
        }
        getCachedVisitor().check(player, packetContainer, checkContextContainer);
        onPostPacketReceive(player, packetContainer);
    }

    public synchronized CheckVisitor<WP> getCachedVisitor() {
        if (cachedVisitor == null) cachedVisitor = getVisitor();
        initializedCheckInstances = true;

        return cachedVisitor;
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public abstract CheckVisitor<WP> getVisitor();

    public abstract boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container);

    public abstract void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer);

}
