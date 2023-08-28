package me.alek.honey.checks;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends SyncPacketAdapter<WP> implements IPacketListener {

    private CheckVisitor<WP> cachedVisitor;

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
            defaultDevelopmentData.addValue(" Packet", packetContainer.getHandle().getClass().getSimpleName());
            defaultDevelopmentData.addValue(" Version", Bukkit.getServer().getVersion());
        }

        if (!onPrePacketReceive(player, packetContainer, checkContextContainer)) {
            return;
        }
        getCachedVisitor().check(this, player, packetContainer, checkContextContainer);
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

    public abstract ListenerPriority getPriority();
}
