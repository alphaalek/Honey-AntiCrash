package me.alek.honey.netty.packetwrappers.play.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;

import java.util.ArrayList;
import java.util.List;

public class WrappedPlayInUpdateSign extends ObjectWrappedPacket<WrappedPlayInUpdateSign> {

    private static Class<?> iChatBaseComponentArrayClass;
    private static MethodInvoker<?> textComponentGetter;

    static {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            iChatBaseComponentArrayClass = Reflection.getArrayClass(NMS.getIChatBaseComponentClass());

            textComponentGetter = Reflection.getMethod(NMS.getIChatBaseComponentClass(), "getText");
        }
    }

    public WrappedPlayInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String[] getLines() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            return getContainer().getStructure().getStringArrays().readField(0);
        }

        Object[] chatBaseComponentArray = getChatBaseComponents();
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = (String) textComponentGetter.invoke(chatBaseComponentArray[i]);
        }

        return lines;
    }

    private Object[] getChatBaseComponents() {
        return (Object[]) getContainer().getStructure().getObjectsOf(iChatBaseComponentArrayClass).readField(0);
    }

    public List<JsonObject> getLegacyJsonPages() {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
            return new ArrayList<>();
        }

        List<JsonObject> pages = new ArrayList<>();
        for (Object chatBaseComponent : getChatBaseComponents()) {
            if (!NMS.getIChatBaseComponentClass().isAssignableFrom(chatBaseComponent.getClass())) {
                continue;
            }

            try {
                JsonElement json = ItemReflection.chatComponentToJson(chatBaseComponent);
                if (json.isJsonObject()) {
                    pages.add(json.getAsJsonObject());
                } else if (json.isJsonPrimitive()) {
                    JsonObject page = new JsonObject();
                    page.add("text", json);
                    pages.add(page);
                } else {
                    throw new IllegalStateException("Illegal json type: " + json);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return pages;
    }
}
