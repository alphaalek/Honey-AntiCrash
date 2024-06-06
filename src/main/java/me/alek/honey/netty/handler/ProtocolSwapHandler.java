package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;

public class ProtocolSwapHandler {

    private PacketState updateAfterEventloop;
    private PacketBound updateDirection;

    public void swapProtocolIfNeeded(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (updateAfterEventloop != null && updateDirection != null) {
            setProtocolState(packet.getChannel(), updateAfterEventloop, updateDirection);
            updateAfterEventloop = null;
            updateDirection = null;
        }

        if (packet.getType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                updateAfterEventloop = PacketState.PLAY;
            } else {
                updateAfterEventloop = PacketState.CONFIGURATION;
            }
            updateDirection = PacketBound.CLIENT;
        } else if (packet.getType() == PacketType.Configuration.Server.FINISH_CONFIGURATION
                || packet.getType() == PacketType.Play.Server.START_CONFIGURATION) {
            updateAfterEventloop = PacketState.PLAY;
            updateDirection = PacketBound.CLIENT;
        } else if (packet.getType() == PacketType.Login.Client.LOGIN_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.SERVER);
        } else if (packet.getType() == PacketType.Play.Client.CONFIGURATION_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.SERVER);
        } else if (packet.getType() == PacketType.Configuration.Client.FINISH_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.PLAY, PacketBound.SERVER);
        } else if (packet.getType() == PacketType.Play.Server.LOGIN) {
            setProtocolState(packet.getChannel(), PacketState.PLAY, null);
        } else if (packet.getType() == PacketType.Handshake.Client.SET_PROTOCOL) {
            JavaConverter<Enum<?>, WrappedEnumProtocol> converter = Converters.getEnumProtocolConverter();
            int packetStateOrdinal;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_1)) {
                WrappedEnumProtocol protocol = packet.getModifier(NMS.getEnumProtocolClass(), converter, PacketBound.SERVER).readField(0);
                packetStateOrdinal = ((Enum<?>)protocol.getHandle()).ordinal();
            } else {
                Object clientIntent = packet.getModifier(NMS.getClientIntentClass()).readField(0);
                int clientIntentOrdinal = ((Enum<?>)clientIntent).ordinal();
                packetStateOrdinal = clientIntentOrdinal == 0 ? /* STATUS */ 2 : /* LOGIN */ 3;
           }
            PacketState packetState = PacketState.values()[packetStateOrdinal];
            setProtocolState(packet.getChannel(), packetState, PacketBound.SERVER);
        }
    }

    private void setProtocolState(Channel channel, PacketState state, PacketBound bound)  {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).setProtocolState(bound, state);
        } catch (MissingHandlerException ex) {
            channel.close();
        }
    }
}
