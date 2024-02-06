package me.alek.honey.checks.types;

import me.alek.honey.netty.packet.PacketBundleFilter;

public interface HoneyCheck {

    String getName();

    String getDescription();

    PacketBundleFilter getDumpFilter();
}
