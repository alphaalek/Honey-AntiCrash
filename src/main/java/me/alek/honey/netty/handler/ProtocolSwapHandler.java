package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;

public class ProtocolSwapHandler {

    private PacketState updateAfterEventloop;
    private PacketBound updateBound;

    public void swapProtocolIfNeeded(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (updateAfterEventloop != null) {
            setProtocolState(packet.getChannel(), updateAfterEventloop, updateBound);
            updateAfterEventloop = null;
            updateBound = null;
        }

        if (packet.getType() == PacketTypes.Login.Server.LOGIN_SUCCESS) {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                updateAfterEventloop = PacketState.PLAY;
            } else {
                updateAfterEventloop = PacketState.CONFIGURATION;
                updateBound = PacketBound.CLIENT;
            }
        } else if (packet.getType() == PacketTypes.Configuration.Server.FINISH_CONFIGURATION
                || packet.getType() == PacketTypes.Play.Server.START_CONFIGURATION) {
            updateAfterEventloop = PacketState.PLAY;
            updateBound = PacketBound.CLIENT;
        } else if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.SERVER);
        } else if (packet.getType() == PacketTypes.Play.Client.CONFIGURATION_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.SERVER);
        } else if (packet.getType() == PacketTypes.Configuration.Client.FINISH_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.PLAY, PacketBound.SERVER);
        } else if (packet.getType() == PacketTypes.Handshake.Client.SET_PROTOCOL) {
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
            setProtocolState(packet.getChannel(), packetState, null);
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
