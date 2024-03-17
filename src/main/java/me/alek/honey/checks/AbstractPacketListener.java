package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends SyncPacketAdapter<WP> implements IPacketListener {

    private final UUID commonUUID = UUID.randomUUID();
    private final Map<UUID, CheckVisitor<WP>> visitors = new HashMap<>();
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
                    if (onViolation != null) onViolation.run();

                    CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

                    String packet = packetContainer.getHandle().getClass().getSimpleName();
                    defaultDevelopmentData.addValue(" Packet", packet);
                    applyCommonDevelopmentData(defaultDevelopmentData);
                });
            });
        }
        if (!onPrePacketReceive(player, packetContainer, checkContextContainer, callback)) {
            return;
        }
        getCachedVisitor(player).check(player, packetContainer, checkContextContainer, callback);
        onPostPacketReceive(player, packetContainer, checkContextContainer);
    }

    public synchronized CheckVisitor<WP> getCachedVisitor(HoneyPlayer player) {
        return getCachedVisitor(player, player != null ? player.uuid : commonUUID);
    }

    public synchronized CheckVisitor<WP> getCachedVisitor(HoneyPlayer player, UUID uuid) {
        if (!visitors.containsKey(uuid)) {
            visitors.put(uuid, getVisitor());
            if (player != null)
                player.leaveCallback.addAction(p -> visitors.remove(p.uuid));
        }
        initializedCheckInstances = true;

        return visitors.get(uuid);
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public abstract CheckVisitor<WP> getVisitor();

    public abstract boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback);

    public abstract void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container);

}
