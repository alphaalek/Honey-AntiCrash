package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.EncryptionA;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EncryptionBegin extends AbstractPacketListener<WrappedLoginInEncryptionResponse> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedLoginInEncryptionResponse> getVisitor() {
        CheckVisitor<WrappedLoginInEncryptionResponse> visitor = new CheckVisitor<>();
        visitor.visit(new EncryptionA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.ENCRYPTION_RESPONSE);
    }
}
