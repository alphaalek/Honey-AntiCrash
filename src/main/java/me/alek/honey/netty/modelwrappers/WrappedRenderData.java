package me.alek.honey.netty.modelwrappers;

import org.bukkit.map.MapCursor;

import java.util.ArrayList;

public class WrappedRenderData extends WrappedObject<WrappedRenderData> {

    public WrappedRenderData(Object handle) {
        super(handle);
    }

    public byte[] getBuffer() {
        return getModifier(byte[].class).readField(0);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<MapCursor> getMapCursors() {
        return getModifier(ArrayList.class).readField(0);
    }
}
