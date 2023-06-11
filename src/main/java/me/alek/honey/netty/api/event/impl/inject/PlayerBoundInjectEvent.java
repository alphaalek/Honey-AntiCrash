package me.alek.honey.netty.api.event.impl.inject;

import org.bukkit.entity.Player;

public class PlayerBoundInjectEvent extends InjectEvent {

    private final Player player;

    public PlayerBoundInjectEvent(Player player, InjectType injectType, InjectCallback callback, InjectBound injectBound) {
        super(injectType, callback, injectBound);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
