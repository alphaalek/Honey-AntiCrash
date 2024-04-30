package me.alek.honey.packetwrappers.play.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;

public class WrappedPlayInUpdateSign extends WrappedPacket<WrappedPlayInUpdateSign> {

    private static Class<?> iChatBaseComponentArrayClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> chatSerializerClass;
    private static MethodInvoker<?> textComponentGetter;

    static {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            iChatBaseComponentClass = Reflection.getClass("{nms}.IChatBaseComponent");
            iChatBaseComponentArrayClass = Reflection.getArrayClass("{nms}.IChatBaseComponent");
            chatSerializerClass = Reflection.getClass("{nms}.IChatBaseComponent$ChatSerializer");

            textComponentGetter = Reflection.getMethod(iChatBaseComponentClass, "getText");
        }
    }

    public WrappedPlayInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String[] getLines() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            return getContainer().getStringArrays().readField(0);
        }

        Object[] chatBaseComponentArray = getChatBaseComponents();
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = (String) textComponentGetter.invoke(chatBaseComponentArray[i]);
        }

        return lines;
    }

    private Object[] getChatBaseComponents() {
        return (Object[]) getContainer().getObjects(iChatBaseComponentArrayClass).readField(0);
    }

    public List<JsonObject> getLegacyJsonPages() {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
            return new ArrayList<>();
        }

        Gson gson = ReflectStructureCache.acquireStructure(chatSerializerClass)
                .withType(Gson.class)
                .withTarget(null)
                .readField(0);

        List<JsonObject> pages = new ArrayList<>();
        for (Object chatBaseComponent : getChatBaseComponents()) {
            if (!iChatBaseComponentClass.isInstance(chatBaseComponent)) {
                continue;
            }

            try {
                JsonElement json = gson.toJsonTree(chatBaseComponent);
                if (json.isJsonObject()) {
                    pages.add(json.getAsJsonObject());
                }
                else {
                    JsonObject page = new JsonObject();
                    page.add("text", json);
                    pages.add(page);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return pages;
    }
}
