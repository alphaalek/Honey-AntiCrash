package me.alek.honey.netty.packet.cancellables;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CancellableByteBufImpl implements CancellablePacketObserver {

    private final ByteBuf byteBuf;
    private final Player player;

    public CancellableByteBufImpl(Player player, ByteBuf byteBuf) {
        this.player = player;
        this.byteBuf = byteBuf;
    }

    @Override
    public void cancel() {
        HoneyPlugin.get().getNotificationService().showCancellingPackets(player);
        Bukkit.broadcastMessage("cancelling bytebuf");
        if (byteBuf.refCnt() > 0) byteBuf.clear();
    }
}
