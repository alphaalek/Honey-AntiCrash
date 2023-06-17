package me.alek.honey.checks.types;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public interface ContainerCheck<WP extends WrappedPacket<WP>> extends Check {

    ResponseContainer accept(Player player, PacketContainer<WP> packet, CheckContextContainer container);
}
