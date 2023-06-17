package me.alek.honey.checks;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends AsyncPacketAdapter<WP> {

    private CheckVisitor<WP> visitor;

    public void onPacketReceive(Player player, PacketContainer<WP> packetContainer) {
        if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
            return;
        }
        CheckContextContainer checkContextContainer = new CheckContextContainer();
        if (!onPrePacketReceive(player, packetContainer, checkContextContainer)) {
            return;
        }
        if (visitor == null) {
            visitor = getVisitor();
        }
        visitor.check(this, player, packetContainer, checkContextContainer);
        onPostPacketReceive(player, packetContainer);
    }

    public CheckVisitor<WP> getCachedVisitor() {
        if (visitor == null) {
            visitor = getVisitor();
        }
        return visitor;
    }

    public abstract CheckVisitor<WP> getVisitor();

    public abstract boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container);

    public abstract void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer);
}
