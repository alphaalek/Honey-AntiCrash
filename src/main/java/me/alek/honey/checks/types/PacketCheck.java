package me.alek.honey.checks.types;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public interface PacketCheck<WP extends WrappedPacket<WP>> extends HoneyCheck {

    ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception;
}
