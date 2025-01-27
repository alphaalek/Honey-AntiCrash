package me.alek.honey.netty.packet.type;

import me.alek.honey.util.id.IntIdentifiable;
import me.alek.honey.util.LazyInit;

public interface PacketType extends IntIdentifiable {

    PacketType UNKNOWN = new PacketType() {
        @Override
        public Class<?> getNmsClass() {
            return null;
        }

        @Override
        public LazyInit<Integer> getMinecraftID() {
            return null;
        }

        @Override
        public String getKeyName() {
            return null;
        }

        @Override
        public int getHoneyID() {
            return -1;
        }

        @Override
        public PacketState getState() {
            return PacketState.UNKNOWN;
        }

        @Override
        public PacketBound getBound() {
            return null;
        }
    };

    @Override
    default Integer getID() {
        return getHoneyID();
    }

    Class<?> getNmsClass();

    LazyInit<Integer> getMinecraftID();

    String getKeyName();

    int getHoneyID();

    PacketState getState();

    PacketBound getBound();
}
