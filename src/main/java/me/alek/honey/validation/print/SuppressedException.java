package me.alek.honey.validation.print;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class SuppressedException {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final long COOLDOWN_MILLIS = 1000;

    private int amount;
    private long cooldownUntil = System.currentTimeMillis() - COOLDOWN_MILLIS;
    private final Throwable initialThrown;
    private final Instant initialThrownTime = new Date().toInstant();

    public SuppressedException(Throwable thrown) {
        this.initialThrown = thrown;
    }
    
    public Throwable getInitialThrown() {
        return this.initialThrown;
    }

    public Instant getInitialThrownTime() {
        return this.initialThrownTime;
    }

    public void suppress(PacketContainer<? extends WrappedPacket<?>> packet, Throwable thrown) {
        this.amount++;

        if (System.currentTimeMillis() <= this.cooldownUntil) {
            return;
        }
        this.cooldownUntil = System.currentTimeMillis() + COOLDOWN_MILLIS;

        System.out.println("Exception in thread \"" + Thread.currentThread().getName() + "\"" + (thrown.getMessage() != null ? ": " + thrown.getMessage() : ""));
        if (!PacketTypes.isUnknown(packet.getType())) {
            System.out.println("\tSuppressed exception (" + this.amount + " time(s)) while handling packet " + packet.getType()
                    + " (ID: " + packet.getType().getMinecraftID() + ") "
                    + "by " + packet.getPlayerName() + ", "
                    + "Properties: " + HoneyPlugin.get().getLoggerService().filterTooMuchData(HoneyPlugin.get().getLoggerService().formatPacketEntries(packet)));
        } else {
            System.out.println("\tSuppressed exception (" + this.amount + " time(s)) while handling unknown packet "
                    + "by " + packet.getPlayerName());
        }
        System.out.println("\tInitial thrown at " + FORMATTER.format(this.initialThrownTime));
    }
}
