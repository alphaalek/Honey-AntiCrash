package me.alek.honey.netty.handler;

import me.alek.honey.HoneyPlayer;

public interface UpdatableChannel {

    void setPlayer(HoneyPlayer player);

    HoneyPlayer getPlayer();
}
