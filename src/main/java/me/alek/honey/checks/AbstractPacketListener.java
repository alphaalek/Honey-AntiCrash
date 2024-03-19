package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.utility.Array;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends SyncPacketAdapter<WP> implements IPacketListener, CheckVisitable {

    private final Array<CheckVisitor<?>> visitors = new Array<>();
    private boolean initializedCheckInstances;

    public static void applyCommonDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
        developmentData.addValue(" Version", Bukkit.getServer().getVersion());
        developmentData.removeLastInvisibleData();
        developmentData.addEmpty();
        developmentData.addHeader("Description", null);

        // temporary placeholders
        developmentData.addEmpty(); // description
        developmentData.addEmpty(); // STQ
        developmentData.addEmpty(); // NTQ
    }

    public static void applyQuotients(HoneyPlayer player, CheckContextContainer.DevelopmentData developmentData) {
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

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        CallbackAction<ResponseContainer> callback = new CallbackAction<>();
        if (player != null) {
            callback.addAction((response) -> {
                response.builder().onViolation(onViolation -> {
                    CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

                    if (defaultDevelopmentData.getDataSet().isEmpty()) {
                        String packet = packetContainer.getHandle().getClass().getSimpleName();
                        defaultDevelopmentData.addEmpty();
                        defaultDevelopmentData.addValue(" Packet", packet);
                        applyCommonDevelopmentData(defaultDevelopmentData);
                    }

                    if (onViolation != null) onViolation.run();
                });
            });
        }
        if (!onPrePacketReceive(player, packetContainer, checkContextContainer, callback)) {
            return;
        }
        ((CheckVisitor<WP>)getCachedVisitor(visitors, this, player))
                .check(player, packetContainer, checkContextContainer, callback);

        onPostPacketReceive(player, packetContainer, checkContextContainer);
    }

    public static synchronized CheckVisitor<?> getCachedVisitor(Array<CheckVisitor<?>> visitors, CheckVisitable visitable, HoneyPlayer player) {
        return getCachedVisitor(visitors, visitable, player, player != null ? player.registryID : 0);
    }

    public static synchronized CheckVisitor<?> getCachedVisitor(Array<CheckVisitor<?>> visitors, CheckVisitable visitable, HoneyPlayer player, int registryID) {
        if (!visitors.has(registryID)
                || player != null && !player.equals(visitors.get(registryID).getPlayer())) {
            visitors.set(registryID, visitable.getVisitor(player));

            if (player != null)
                player.leaveCallback.addAction(p -> visitors.remove(registryID));
        }
        visitable.setInitializedCheckInstances();

        return visitors.get(registryID);
    }

    public void setInitializedCheckInstances() {
        initializedCheckInstances = true;
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public abstract boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback);

    public abstract void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container);

}
