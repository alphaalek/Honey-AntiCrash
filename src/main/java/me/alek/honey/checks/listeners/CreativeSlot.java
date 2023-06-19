package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeSlot extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInSetCreativeSlot> getVisitor() {
        CheckVisitor<WrappedPlayInSetCreativeSlot> visitor = new CheckVisitor<>();
        visitor.visit(new CreativeA());
        visitor.visit(new CreativeB());
        visitor.visit(new CreativeC());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.SET_CREATIVE_SLOT);
    }

    public static class CreativeA implements PacketCheck<WrappedPlayInSetCreativeSlot> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "CreativeA";
        }
    }

    public static class CreativeB implements PacketCheck<WrappedPlayInSetCreativeSlot> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            int slot = packet.getSlot();
            if (slot > 47 || (slot < -1 && slot != -999)) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "CreativeB";
        }
    }

    public static class CreativeC implements PacketCheck<WrappedPlayInSetCreativeSlot> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (NBTHelper.checkBukkitNBT(packet.getItem())) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .confiscate(packet.getItem())
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "CreativeC";
        }
    }
}
