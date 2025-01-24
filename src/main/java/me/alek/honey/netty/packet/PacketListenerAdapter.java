package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class PacketListenerAdapter<WP extends SourcedWrappedPacket<WP>> implements PacketListener<WP> {

    public void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }

    public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }

    public void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }
    
    public abstract static class InboundCommon<WP extends SourcedWrappedPacket<WP>> extends PacketListenerAdapter<WP> {
        
        public abstract void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception;

        @Override
        public void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
            onPacketReceive(player, packet);
        }
        
        @Override
        public void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
            onPacketReceive(player, packet);
        }
    }

    public abstract static class Common<WP extends SourcedWrappedPacket<WP>> extends PacketListenerAdapter<WP> {

        public abstract void onPacketStream(HoneyPlayer player, PacketBound bound, PacketContainer<WP> packet) throws Exception;

        @Override
        public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
            onPacketStream(player, PacketBound.CLIENT, packet);
    }
        
        @Override
        public void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
            onPacketStream(player, PacketBound.SERVER, packet);
        }

        @Override
        public void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
            onPacketStream(player, PacketBound.SERVER, packet);
        }
    }
}
