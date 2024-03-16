package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.packetwrappers.play.client.common.WrappedAbstractPositional;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.utility.Locations;
import me.alek.honey.utility.SampleDecayingList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class PositionListener<WP extends WrappedAbstractPositional<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Location> lastTeleportationLocationMap = new HashMap<>();
    private static final Map<UUID, Location> lastPacketLocation = new HashMap<>();
    private static final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(null);

    public PositionListener() {
        HoneyPlugin.get().getPacketLibrary().addListener(new SyncPacketAdapter<WrappedPlayOutPosition>() {

            @Override
            public void onPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutPosition> packetContainer) {
                lastTeleportationLocationMap.put(player.uuid, packetContainer.getPacket().getLocation());
            }

        }, ListenerPriority.HIGHEST, () -> PacketType.Play.Server.POSITION);
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.VEHICLE_MOVE,
                PacketType.Play.Client.GROUND,
                PacketType.Play.Client.FLYING
        );
    }

    public static String format(double value) {
        return (Math.ceil(value * 100) / 100) + "";
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        final Location currentLocation = packetContainer.getPacket().getLocation();
        final Location lastLocation = lastPacketLocation.getOrDefault(player.uuid, currentLocation);

        // check if the packet has any valid location to be checked, or just origin location
        // if it does, ignore this packet and avoid adding this location to the last packet location lookup
        if (Locations.calculate3DDistance(currentLocation, originLocation) < 10) return false;
        lastPacketLocation.put(player.uuid, currentLocation);

        // check if the player has moved by checking if the last location is equal to the current location
        if (Locations.compare3DLocation(currentLocation, lastLocation)) return false;

        // sharing this last location with the pipeline, so other checks can take use of it
        container.getStorage().put(Location.class, lastLocation);

        callback.addAction((response -> {
            Runnable priorOnViolation = response.ON_VIOLATION;
            response.builder().onViolation(() -> {
                final String x = format(currentLocation.getX());
                final String y = format(currentLocation.getY());
                final String z = format(currentLocation.getZ());

                container.getDefaultDevelopmentData().addPoint("Position", x + ", " + y + ", " + z);
                container.getDefaultDevelopmentData().addPoint("Delta Movement", Locations.calculate3DDistance(currentLocation, lastLocation));

                if (priorOnViolation != null)
                    priorOnViolation.run();
            });
        }));

        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

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
        visitor.visit(new PositionE<>());
        visitor.visit(new PositionF<>());
        visitor.visit(new PositionG<>());
        visitor.visit(new PositionH<>());
        visitor.visit(new PositionI<>());
        visitor.visit(new PositionJ<>());
        return visitor;
    }

    @Override
    public void cleanup(HoneyPlayer player) {
        final UUID uuid = player.uuid;

        lastPacketLocation.remove(uuid);
        lastTeleportationLocationMap.remove(uuid);
    }

    public static Location getLastLocationFor(HoneyPlayer player) {
        return lastPacketLocation.getOrDefault(player.uuid, player.getLocation());
    }

    public static boolean isChunkLoaded(Location location) {
        final int x = location.getBlockX() >> 4;
        final int z = location.getBlockZ() >> 4;

        return location.getWorld().isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordIntPair getChunkAt(Location location) {
        if (isChunkLoaded(location)) {
            final int x = location.getBlockX() >> 4;
            final int z = location.getBlockZ() >> 4;

            return new WrappedChunkCoordIntPair(x, z);
        }
        return null;
    }

    public static boolean isAnyLatelyTeleportationLocationsCloseTo(HoneyPlayer player, double diff, Function<Location, Double> distanceFunction) {
        if (lastTeleportationLocationMap.containsKey(player.uuid)) {
            Location lastTeleportationLocation = lastTeleportationLocationMap.get(player.uuid);
            double distance = distanceFunction.apply(lastTeleportationLocation);

            return distance != -1 && distance < diff;
        }
        return false;
    }

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private boolean wasOnGround;

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (!(packet.getType() == PacketType.Play.Client.FLYING || packet.getType() == PacketType.Play.Client.GROUND))
                return ResponseBuilder.SUCCESS;

            if (getSetting().isEnabled()) {

                boolean wasOnGround = this.wasOnGround;
                this.wasOnGround = packet.onGround();

                if (packet.onGround() != wasOnGround) {
                    return ResponseBuilder.inst().terminate().flag().build();
                }
            }
            return ResponseBuilder.inst().terminate().build();
        }

        @Override
        public int getID() {
            return 41;
        }

        @Override
        public String getName() {
            return "Position.A";
        }

        @Override
        public String getDescription() {
            return "Switching between ground and air too quickly";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionA.class);
        }
    }

    public static class PositionB<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            final Location packetLoc = packet.getLocation();
            final Location lastKnownPacketLoc = container.getStorage().get(Location.class, 0);

            if (isAnyLatelyTeleportationLocationsCloseTo(
                    player, 15,
                    (location) -> Locations.calculate3DDistance(location, packetLoc))) {
                return ResponseBuilder.SUCCESS.builderCopy().skip(3).build();
            }

            final double distance2d = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distance2d);

            final double distanceY = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distanceY);

            if (getSetting().isEnabled()) {
                final double maxDistance = getSetting().max;
                // 2d distance check
                if (distance2d >= maxDistance && distance2d != 0) {
                    return ResponseBuilder.inst().cancel().flag().withViolationKey(new ViolationService.CheckKey("2d"))
                            .onViolation(() -> container.getDevelopmentData(this).addPoint("2D Distance", distance2d))
                            .build();
                }
                // y distance check
                if (distanceY >= maxDistance && distanceY != 0) {
                    return ResponseBuilder.inst().cancel().flag().withViolationKey(new ViolationService.CheckKey("y"))
                            .onViolation(() -> container.getDevelopmentData(this).addPoint("Y Distance", distanceY))
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 42;
        }

        @Override
        public String getName() {
            return "Position.B";
        }

        @Override
        public String getDescription() {
            return "Too high delta movement";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SHORT_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionB.class);
        }
    }

    public static class PositionC<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private Location lastFlaggedLocation;

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // check if the player is flying, because then the buffer will be filled up nevertheless
                if (player.player.isFlying())
                    return ResponseBuilder.SUCCESS;

                // ignores when you fall down. note, that if it would be at 0, it will also ignore when you
                // walk around on ground, because the gravity affect the player's velocity
                if (player.player.getVelocity().getY() < -1.5)
                    return ResponseBuilder.SUCCESS;

                final Location priorLocation = container.getStorage().get(Location.class, 0);
                // return if it's the first location being sent
                if (priorLocation == null)
                    return ResponseBuilder.SUCCESS;

                // ignores packets close to the recent flag
                if (lastFlaggedLocation != null) {
                    if (Locations.calculate3DDistance(lastFlaggedLocation, packet.getLocation()) < 1.5)
                        return ResponseBuilder.SUCCESS;
                }
                lastFlaggedLocation = packet.getLocation();

                final double priorDistance = Locations.calculate3DDistance(priorLocation, packet.getLocation());
                return ResponseBuilder.inst().flag(Math.min(10, priorDistance)).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 43;
        }

        @Override
        public String getName() {
            return "Position.C";
        }

        @Override
        public String getDescription() {
            return "Buffer of position distance exceeded";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionC.class);
        }
    }

    public static class PositionD<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private WrappedChunkCoordIntPair lastChunk;
        private boolean manipulatedFlightSettings;

        private final ViolationService.CheckKey flightKey = new ViolationService.CheckKey("Position.B", true);
        private final ViolationService.CheckKey nonFlightKey = new ViolationService.CheckKey("Position.B", false);

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {

            final ViolationService.CheckKey key = (player.player.isFlying()) ? flightKey : nonFlightKey;

            // make a copy of settings to when you fly, because you can visit way more chunks then
            if (!manipulatedFlightSettings) {
                final double maxPerInterval = getSetting().maxPerInterval;
                final ConfigurationOptions.VlSetting setting = getSetting().clone();
                setting.maxPerInterval = maxPerInterval * 3;

                // force a new packet limit handler to be created with the new flight setting
                HoneyPlugin.get().getViolationService().getDistributor(player, flightKey, setting);

                manipulatedFlightSettings = true;
            }
            // gets the chunk of the player's location which should be compared to the last chunk of the player, and this way
            // consider if the player is moving too fast around the map, and maybe trying to crash the server by loading too many chunks
            final WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());
            if (chunk != null) {
                // the chunk is loaded, and we have to make sure that all responses will skip the next check in the pipeline

                if (!getSetting().isEnabled() || lastChunk != null && lastChunk.equals(chunk)) {
                    // the player is in the same chunk as the last position update
                    return ResponseBuilder.SUCCESS.builderCopy().skip(1).build();
                }
                // the player has moved into a new chunk, and we have to register this chunk as the current one
                lastChunk = chunk;
                return ResponseBuilder.inst().flag().skip(1).withLimitsKey(key).build();
            }
            // the chunk is not loaded and therefore Position.E should be flagged by not returning a skip response
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 44;
        }

        @Override
        public String getName() {
            return "Position.D";
        }

        @Override
        public String getDescription() {
            return "Too many chunks discovered";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionD.class);
        }
    }

    public static class PositionE<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().cancel().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 45;
        }

        @Override
        public String getName() {
            return "Position.E";
        }

        @Override
        public String getDescription() {
            return "Too many unloaded chunks discovered";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionE.class);
        }
    }

    public static class PositionF<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<Direction, Axis> axisLookup = new EnumMap<>(Direction.class);

        private enum Direction {
            UP, DOWN, NORTH, SOUTH, WEST, EAST, STILL
        }

        private enum Axis {
            HORIZONTAL_1(Direction.NORTH, Direction.SOUTH),
            HORIZONTAL_2(Direction.WEST, Direction.EAST),
            VERTICAL(Direction.UP, Direction.DOWN);

            private final Direction opp1;
            private final Direction opp2;

            public Direction getOther(Direction dir) {
                return (dir == opp1) ? opp2 : opp1;
            }

            Axis(Direction opp1, Direction opp2) {
                this.opp1 = opp1;
                this.opp2 = opp2;
            }
        }

        static {
            for (Axis axis : Axis.values()) {
                axisLookup.put(axis.opp1, axis);
                axisLookup.put(axis.opp2, axis);
            }
        }

        private Direction[] calculateVelocity(Location loc1, Location loc2) {
            final Direction[] velocity = new Direction[3];

            final double deltaX = loc1.getX() - loc2.getX();
            velocity[0] = (deltaX > 0) ? Direction.EAST : (deltaX == 0) ? Direction.STILL : Direction.WEST;

            final double deltaY = loc1.getY() - loc2.getY();
            velocity[1] = (deltaY > 0) ? Direction.DOWN : (deltaY == 0) ? Direction.STILL : Direction.UP;

            final double deltaZ = loc1.getZ() - loc2.getZ();
            velocity[2] = (deltaZ > 0) ? Direction.SOUTH : (deltaZ == 0) ? Direction.STILL : Direction.NORTH;

            return velocity;
        }

        private Direction[] lastDirectionFlow;

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                final Location priorLocation = container.getStorage().get(Location.class, 0);
                if (priorLocation == null) // wait for next position
                    return ResponseBuilder.SUCCESS;

                final Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                final Direction[] priorVelocity = lastDirectionFlow;

                lastDirectionFlow = velocity;
                if (priorVelocity != null && priorVelocity.length == velocity.length) {

                    int i = -1;
                    final ResponseBuilder builder = ResponseBuilder.inst();
                    for (final Direction dir : velocity) {

                        // make sure that there aren't any discrepancy in the lengths and indices of the different velocities
                        if (++i == priorVelocity.length) break;

                        final Axis axis = axisLookup.get(dir);
                        final Direction corr = priorVelocity[i];

                        if (corr == Direction.STILL || dir == Direction.STILL) continue;

                        if (corr == axis.getOther(dir)) {
                            builder.add(ResponseBuilder.inst().flag().withViolationKey(
                                    new ViolationService.CheckKey(axis.name().toLowerCase())).build());
                        }
                    }
                    final ResponseContainer response;
                    if (!(response = builder.build()).SUB_CONTAINERS.isEmpty())
                        return response;
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 46;
        }

        @Override
        public String getName() {
            return "Position.F";
        }

        @Override
        public String getDescription() {
            return "Switching directions too quickly";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionF.class);
        }
    }

    public static class PositionG<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (packet.getType() == PacketType.Play.Client.VEHICLE_MOVE
                        && player.player.getVehicle() == null) {
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 47;
        }

        @Override
        public String getName() {
            return "Position.G";
        }

        @Override
        public String getDescription() {
            return "Moving vehicle without being in one";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SHORT_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionG.class);
        }
    }

    public static class PositionH<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private double guessedDelta2D;
        private double guessedDeltaY;

        @Override
        public synchronized ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            final ResponseContainer response = ResponseBuilder.SUCCESS.builderCopy().skip(1).build();
            if (getSetting().isEnabled()) {

                final double delta2D = container.getStorage().getDouble(0);
                final double deltaY = container.getStorage().getDouble(1);

                if (delta2D == 0 && deltaY == 0) return response;

                boolean hasPattern = false;
                final ResponseBuilder builder = ResponseBuilder.inst();

                if (delta2D > 0.05) {
                    if (guessedDelta2D == delta2D) {
                        builder.add(
                                ResponseBuilder.inst().flag()
                                        .withViolationKey(new ViolationService.CheckKey("2d"))
                                        .build()
                        );
                        hasPattern = true;
                    }
                    guessedDelta2D = delta2D;
                }
                if (deltaY > 0.5) {
                    if (guessedDeltaY == deltaY) {
                        final Material material = player.player.getEyeLocation().getBlock().getType();
                        if (material.name().contains("WATER") || material.name().contains("LAVA")
                                || deltaY % 0.375 < 0.01) {
                            return response;
                        }
                        builder.add(
                                ResponseBuilder.inst().flag()
                                        .withViolationKey(new ViolationService.CheckKey("y"))
                                        .build()
                        );
                        hasPattern = true;
                    }
                    guessedDeltaY = deltaY;
                }
                if (hasPattern) return builder.build();
            }
            return response;
        }

        @Override
        public int getID() {
            return 48;
        }

        @Override
        public String getName() {
            return "Position.H";
        }

        @Override
        public String getDescription() {
            return "Too many identical delta movements";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionH.class);
        }
    }

    public static class PositionI<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                return ResponseBuilder.inst().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 49;
        }

        @Override
        public String getName() {
            return "Position.I";
        }

        @Override
        public String getDescription() {
            return "Too many identical delta movements";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionI.class);
        }
    }

    public static class PositionJ<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final double x = packet.getX();
                final double y = packet.getY();
                final double z = packet.getZ();

                if (x > 3.0E7 || x < -3.0E7) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (z > 3.0E7 || z < -3.0E7) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (y < getSetting().min || y > getSetting().max) {
                    if (player.player.isDead())
                        return ResponseBuilder.inst().cancel().build();

                    return ResponseBuilder.inst().cancel().flag().onPunish(() -> {
                        double yPosFix = y;
                        if (y < getSetting().min) {
                            while (player.getWorld().getBlockAt((int) x, (int) yPosFix++, (int) z).getType() != Material.AIR || yPosFix < 0) {
                                if (yPosFix > getSetting().max - 3) break;
                            }
                        }
                        if (y > getSetting().max) {
                            while (player.getWorld().getBlockAt((int) x, (int) yPosFix--, (int) z).getType() == Material.AIR) {
                                if (yPosFix < 16) break;
                            }
                            yPosFix += 2;
                        }
                        player.player.teleport(new Location(player.getWorld(), x, yPosFix, z));
                    }).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 50;
        }

        @Override
        public String getName() {
            return "Position.J";
        }

        @Override
        public String getDescription() {
            return "Too high up in air or below bedrock";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SHORT_POSITION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionJ.class);
        }
    }
}
