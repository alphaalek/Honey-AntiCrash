package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends SyncPacketAdapter<WP> implements IPacketListener {

    private CheckVisitor<WP> cachedVisitor;
    private static final DecimalFormat formatter = new DecimalFormat("###.####");

    public static void applyCommonDevelopmentData(Player player, CheckContextContainer.DevelopmentData developmentData) {
        developmentData.addValue(" Version", Bukkit.getServer().getVersion());
        developmentData.addEmpty();
        developmentData.addHeader("Description", null);
        developmentData.addEmpty();
        developmentData.addPoint("Netty Thread Quotient", formatter.format(HoneyPlugin.get().getPacketHandleTimeTracker().getTimeToAverageQuotient(player) * 100) + "%");
    }

    public void onPacketReceive(Player player, PacketContainer<WP> packetContainer) throws Exception {
        if (player == null) return;

        if (packetContainer.getState() == PacketState.PLAY) {
            if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
                return;
            }
        }
        CheckContextContainer checkContextContainer = new CheckContextContainer();
        CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

        defaultDevelopmentData.addEmpty();
        defaultDevelopmentData.addValue(" Packet", packetContainer.getHandle().getClass().getSimpleName());
        applyCommonDevelopmentData(player, defaultDevelopmentData);

        if (!onPrePacketReceive(player, packetContainer, checkContextContainer)) {
            return;
        }
        getCachedVisitor().check(player, packetContainer, checkContextContainer);
        onPostPacketReceive(player, packetContainer);
    }

    public CheckVisitor<WP> getCachedVisitor() {
        if (cachedVisitor == null) {
            cachedVisitor = getVisitor();
        }
        return cachedVisitor;
    }

    public abstract CheckVisitor<WP> getVisitor();

    public abstract boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container);

    public abstract void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer);

}
