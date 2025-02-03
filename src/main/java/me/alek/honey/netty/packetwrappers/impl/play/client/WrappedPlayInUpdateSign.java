package me.alek.honey.netty.packetwrappers.impl.play.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemFactory;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;

import java.util.ArrayList;
import java.util.List;

public class WrappedPlayInUpdateSign extends ObjectWrappedPacket<WrappedPlayInUpdateSign> {

    private static Class<?> iChatBaseComponentArrayClass;
    private static MethodInvoker<?> textComponentGetter;

    static {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            iChatBaseComponentArrayClass = Reflection.getArrayClass(MinecraftReflection.getIChatBaseComponentClass());
            textComponentGetter = Reflection.getMethod(MinecraftReflection.getIChatBaseComponentClass(), "getText");
        }
    }

    public WrappedPlayInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packet) {
        super(rawPacket, packet);
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

    public List<JsonObject> getLegacyJsonLines() {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_8_8)) {
            return new ArrayList<>();
        }

        List<JsonObject> lines = new ArrayList<>();
        for (Object component : getChatBaseComponents()) {
            if (!MinecraftReflection.getIChatBaseComponentClass().isAssignableFrom(component.getClass())) {
                continue;
            }

            try {
                JsonElement json = ItemFactory.chatComponentToJson(component);
                if (json.isJsonObject()) {
                    lines.add(json.getAsJsonObject());
                } else if (json.isJsonPrimitive()) {
                    JsonObject page = new JsonObject();
                    page.add("text", json);
                    lines.add(page);
                } else {
                    throw new IllegalStateException("Illegal json type: " + json);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return lines;
    }
}
