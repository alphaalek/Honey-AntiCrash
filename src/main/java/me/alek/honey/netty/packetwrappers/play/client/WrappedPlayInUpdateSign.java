package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.utility.protocol.NetProtocol;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInUpdateSign extends WrappedPacket<WrappedPlayInUpdateSign> {

    private static Class<?> iChatBaseComponentArrayClass;
    private static MethodInvoker<?> formatter;
    private static MethodInvoker<?> textComponentGetter;

    static {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            Class<?> iChatBaseComponentClass = Reflection.getClass("{nms}.IChatBaseComponent");
            iChatBaseComponentArrayClass = Reflection.getArrayClass("{nms}.IChatBaseComponent");

            Class<?> enumChatFormatClass = Reflection.getClass("{nms}.EnumChatFormat");

            formatter = Reflection.getMethod(enumChatFormatClass, "c", String.class);
            textComponentGetter = Reflection.getMethod(iChatBaseComponentClass, "c");
        }
    }

    public WrappedPlayInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String[] getLines() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            return getContainer().getStringArrays().readField(0);
        }
        Object[] chatBaseComponentArray = (Object[]) getContainer().getObjects(iChatBaseComponentArrayClass).readField(0);
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = (String) formatter.invoke(null, textComponentGetter.invoke(chatBaseComponentArray[i]));
        }
        return lines;
    }
}
