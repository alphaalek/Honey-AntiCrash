package me.alek.honey.checks.types;

import me.alek.honey.netty.packet.bundle.PacketBundleFilter;

public interface HoneyCheck {

    int getID();

    String getName();

    String getDescription();

    PacketBundleFilter getDumpFilter();

    default boolean isViolation() {
        return false;
    }
}
