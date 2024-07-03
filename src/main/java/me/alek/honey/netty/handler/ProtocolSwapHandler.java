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
import org.bukkit.Bukkit;

public class ProtocolSwapHandler {

    public void swapProtocolIfNeeded(PacketContainer<? extends WrappedPacket<?>> packet) {
        // login -> play (server -> client) (legacy)
        if (packet.getType() == PacketTypes.Login.Server.LOGIN_SUCCESS
                && NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            for (PacketBound bound : PacketBound.values()) {
                HoneyPacketHandler.EVENTLOOP_PROCESSES[bound.ordinal()].get().add(() -> setProtocolState(packet.getChannel(), PacketState.PLAY, bound));
            }
        // login -> configuration (client -> server)
        } else if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, null);

        // configuration -> play (server -> client)
        } else if (packet.getType() == PacketTypes.Configuration.Server.FINISH_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.PLAY, PacketBound.CLIENT);
        // configuration -> play (client -> server)
        } else if (packet.getType() == PacketTypes.Configuration.Client.FINISH_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.PLAY, PacketBound.SERVER);

        // play -> configuration (server -> client)
        } else if (packet.getType() == PacketTypes.Play.Server.START_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.CLIENT);
        // play -> configuration (client -> server)
        } else if (packet.getType() == PacketTypes.Play.Client.CONFIGURATION_START_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION, PacketBound.SERVER);

        // handshake -> status/login (client -> server)
        } else if (packet.getType() == PacketTypes.Handshake.Client.SET_PROTOCOL) {
            JavaConverter<Enum<?>, WrappedEnumProtocol> converter = Converters.getEnumProtocolConverter();
            int packetStateOrdinal;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_1)) {
                WrappedEnumProtocol protocol = packet.getModifier(NMS.getEnumProtocolClass(), converter, PacketBound.SERVER).readField(0);
                packetStateOrdinal = ((Enum<?>)protocol.getHandle()).ordinal();
            } else {
                Object clientIntent = packet.getModifier(NMS.getClientIntentClass()).readField(0);
                int clientIntentOrdinal = ((Enum<?>)clientIntent).ordinal();
                packetStateOrdinal = clientIntentOrdinal == 0 ? /* status */ 2 : /* login */ 3;
           }
            PacketState packetState = PacketState.values()[packetStateOrdinal];
            setProtocolState(packet.getChannel(), packetState, null);
        }

        // TODO: handle configuration pipeline
    }

    private void setProtocolState(Channel channel, PacketState state, PacketBound bound)  {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).setProtocolState(bound, state);
        } catch (MissingHandlerException ex) {
            channel.close();
        }
    }
}
