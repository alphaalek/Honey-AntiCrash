package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.utility.Locations;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Position<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {


    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return !Locations.compare3DLocation(packetContainer.getPacket().getLocation(), player.getLocation());
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new PositionA<>());
        visitor.visit(new PositionB<>());
        visitor.visit(new PositionC<>());
        visitor.visit(new PositionD<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        PositionA.lastChunkMap.remove(player.getUniqueId());
        PositionC.lastPacketLocation.remove(player.getUniqueId());
    }

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        private static volatile Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Chunk chunk = packet.getWorld().getChunkAt(packet.getLocation());
            container.putChunk(chunk);
            if (lastChunkMap.get(player.getUniqueId()) == chunk) {
                return ResponseBuilder.SUCCESS.builder().skip(1).build(); // skipper unloaded chunk check
            }
            lastChunkMap.put(player.getUniqueId(), chunk);

            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public String getViolationName() {
            return "PositionA";
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.NewChunks.get();
        }
    }

    public static class PositionB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Chunk chunk = container.getChunk(0);
            if (chunk != null && player.getWorld().isChunkLoaded(chunk) && chunk.isLoaded()) {
                return ResponseBuilder.SUCCESS;
            }
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public String getViolationName() {
            return "PositionB";
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.NewUnloadedChunks.get();
        }
    }

    public static class PositionC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        private static volatile Map<UUID, Location> lastPacketLocation = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (packet.getLocation().getChunk() == null) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            final int maxDistance = ConfigurationOptions.Velocity.Movement.MAX_2D_DISTANCE;

            final Location playerLoc = player.getLocation();
            final Location packetLoc = packet.getLocation();
            final Location lastKnownPacketLoc = lastPacketLocation.get(player.getUniqueId());

            if (lastKnownPacketLoc != null) {

                // for at undgå flag ved lagspikes
                final int distance2d_client = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
                final int distanceY_client = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);

                if (distance2d_client < maxDistance && distanceY_client < maxDistance) {
                    return ResponseBuilder.SUCCESS;
                }
            }
            lastPacketLocation.put(player.getUniqueId(), packetLoc);

            final int distance2d = Locations.calculate2DDistance(packetLoc, playerLoc);
            final int distanceY = Locations.calculateYDistance(packetLoc, playerLoc);

            // for at undgå flag ved serverside teleporteringer
            if (distance2d > maxDistance || distanceY > maxDistance) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .flag()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PositionC";
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.Movement.get();
        }
    }

    public static class PositionD<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            final double x = packet.getX();
            final double y = packet.getY();
            final double z = packet.getZ();

            if (x > 3.0E7 || x < -3.0E7 || z > 3.0E7 || z < -3.0E7 || y < ConfigurationOptions.Velocity.Height.MIN_HEIGHT || y > ConfigurationOptions.Velocity.Height.MAX_HEIGHT) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PositionD";
        }
    }

    // PositionE

    // interval, percentage-of-vertical-switch, vls-amount

    // tjekker seneste cached model class "VelocityReference", og sammenligner den direction med
    // den direction som bliver regnet ud ift. til seneste lokation af den VelocityReference
    // hvis det er en anden direction end den tidligere cached, vil det blive flagged som target,
    // ellers vil det blive flagged som normal (forestil dig hvis du hopper, vil alle position packets
    // fra jorden op til det højeste punkt være i et flow af samme direction, men hvis man derimod
    // spammer en masse packets op og ned i et mønster der ikke er normalt i Minecraft, vil det blive
    // flagged som target. Når det interval er ovre, tjekker den om target er højere end det antal af
    // procent som er blevet konfigureret til. jeg tænkte på omkring min. 35-45

    // put det ind i violation service og lav det som en mulighed for andre modules også, hvis jeg tænker
    // at det kunne være en god idé at implementere flere steder i fremtiden. Det samme gælder for
    // buffer violation check, jeg har bare ikke noget konkret sted lige nu at bruge det, men jeg ved det
    // vil blive nyttigt på et tidspunkt

}
