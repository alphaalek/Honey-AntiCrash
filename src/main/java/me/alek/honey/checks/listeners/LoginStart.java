package me.alek.honey.checks.listeners;

import com.mojang.authlib.GameProfile;
import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.LoginA;
import me.alek.honey.checks.impl.LoginB;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginStart extends AbstractPacketListener<WrappedLoginInLoginStart> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedLoginInLoginStart> getVisitor() {
        CheckVisitor<WrappedLoginInLoginStart> visitor = new CheckVisitor<>();
        visitor.visit(new LoginA());
        visitor.visit(new LoginB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.LOGIN_START);
    }

}
