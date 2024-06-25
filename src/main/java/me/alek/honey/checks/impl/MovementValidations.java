package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketValidation;
import me.alek.honey.checks.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class MovementValidations<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> {

    private static final Map<UUID, Location> lastTeleportationLocationMap = new HashMap<>();
    private static final Map<UUID, Location> lastPacketLocation = new HashMap<>();
    private static final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(null);

    public MovementValidations() {
        HoneyPlugin.get().getPacketLibrary().addListener(new PacketAdapter<WrappedPlayOutPosition>() {

            @Override
            public void onPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutPosition> packetContainer) {
                lastTeleportationLocationMap.put(player.uuid, packetContainer.getPacket().getLocation());
            }

        }, ListenerPriority.HIGHEST, () -> PacketTypes.Play.Server.POSITION);
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketTypes.Play.Client.POSITION,
                PacketTypes.Play.Client.POSITION_LOOK,
                PacketTypes.Play.Client.GROUND,
                PacketTypes.Play.Client.FLYING
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
        if (LocationUtils.compareLocation(currentLocation, originLocation)) return false;
        lastPacketLocation.put(player.uuid, currentLocation);

        // check if the player has moved by checking if the last location is equal to the current location
        if (LocationUtils.compare3DLocation(currentLocation, lastLocation)) return false;

        // share this last location with the pipeline, so the checks can take use of it
        container.getStorage().putLocation(lastLocation);

        callback.addAction((response -> {
            response.builder().onViolation((check, onViolation) -> {
                if (onViolation != null) onViolation.run();

                final String x = format(currentLocation.getX());
                final String y = format(currentLocation.getY());
                final String z = format(currentLocation.getZ());

                container.getDevelopmentData(check).addPoint("Position", x + ", " + y + ", " + z);
            });
        }));

        return true;
    }

    @Override
    public CheckPipeline<PacketValidation<WP>> getNewPipeline() {
        return CheckPipeline.from(new MovementA<>(), new MovementB<>(), new MovementC<>(), new MovementD<>(), new MovementE<>(),
                new MovementF<>(), new MovementG<>(), new MovementH<>(), new MovementI<>(), new MovementJ<>());
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

    public static boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordIntPair getChunkAt(Location location) {
        final int x = ((int) location.getX()) >> 4;
        final int z = ((int) location.getZ()) >> 4;

        return new WrappedChunkCoordIntPair(x, z, world -> isChunkLoaded(world, x, z));
    }

    public static boolean isAnyLatelyTeleportationLocationsCloseTo(HoneyPlayer player, double diff, Function<Location, Double> distanceFunction) {
        if (lastTeleportationLocationMap.containsKey(player.uuid)) {
            Location lastTeleportationLocation = lastTeleportationLocationMap.get(player.uuid);
            double distance = distanceFunction.apply(lastTeleportationLocation);

            return distance != -1 && distance < diff;
        }
        return false;
    }

    public static class MovementA<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private boolean wasOnGround;

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (!(packet.getType() == PacketTypes.Play.Client.FLYING || packet.getType() == PacketTypes.Play.Client.GROUND))
                return Validation.PASS;

            if (getSetting().isEnabled()) {

                boolean wasOnGround = this.wasOnGround;
                this.wasOnGround = packet.onGround();

                if (packet.onGround() != wasOnGround) {
                    return Validation.validate().terminate().flag().build();
                }
            }
            return Validation.validate().terminate().build();
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionA.class);
        }
    }

    public static class MovementB<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey XZKey = ViolationService.HoneyKey.of(0, "2d");
        private static final ViolationService.HoneyKey YKey = ViolationService.HoneyKey.of(1, "y");

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            final Location packetLoc = packet.getLocation();
            final Location lastKnownPacketLoc = container.getStorage().getLocation(0);

            if (isAnyLatelyTeleportationLocationsCloseTo(
                    player, 15,
                    (location) -> LocationUtils.calculate3DDistance(location, packetLoc))) {
                return Validation.PASS.copy().skip(3).build();
            }

            final double distance2d = LocationUtils.calculate2DDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distance2d);

            final double distanceY = LocationUtils.calculateYDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distanceY);

            if (getSetting().isEnabled()) {
                final double maxDistance = getSetting().max;
                // 2d distance check
                if (distance2d >= maxDistance && distance2d != 0) {
                    return Validation.validate().cancel().flag().withViolationKey(XZKey)
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("2D Distance", distance2d);
                            })
                            .build();
                }
                // y distance check
                if (distanceY >= maxDistance && distanceY != 0) {
                    return Validation.validate().cancel().flag().withViolationKey(YKey)
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Y Distance", distanceY);
                            })
                            .build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionB.class);
        }
    }

    public static class MovementC<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private Location lastFlaggedLocation;

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                // ignores when you fall down. note, that if it would be at 0, it will also ignore when you
                // walk around on ground, because the gravity affect the player's velocity
                if (player.player.getVelocity().getY() < -1.5)
                    return Validation.PASS;

                final Location priorLocation = container.getStorage().getLocation(0);
                // return if it's the first location being sent
                if (priorLocation == null) return Validation.PASS;

                // ignores packets close to the recent flag
                if (lastFlaggedLocation != null) {
                    if (LocationUtils.calculate3DDistance(lastFlaggedLocation, packet.getLocation()) < 1)
                        return Validation.PASS;
                }
                lastFlaggedLocation = packet.getLocation();

                final double priorDistance = LocationUtils.calculate3DDistance(priorLocation, packet.getLocation());
                return Validation.validate().flag(Math.min(10, priorDistance)).build();
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionC.class);
        }
    }

    public static class MovementD<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private WrappedChunkCoordIntPair lastChunkCameFrom;
        private WrappedChunkCoordIntPair lastChunk;

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            // gets the chunk of the player's location which should be compared to the last chunk of the player, and this way
            // consider if the player is moving too fast around the map and loading excessively chunks
            WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());
            boolean equalsChunk = chunk.equals(lastChunk);
            boolean equalsPreviousChunk = chunk.equals(lastChunkCameFrom);

            int skip = equalsChunk || equalsPreviousChunk ? 1 : chunk.isLoaded(player.getWorld()) ? 1 : 0;

            if (!equalsChunk) {
                lastChunkCameFrom = lastChunk;
                lastChunk = chunk;
            }

            // if the setting is disabled, we will just return SUCCESS
            // or if the player is in the same chunk as the last position or the same chunk as the previous chunk
            // the skip is for letting the next check in the check pipeline (Position.E), either flag or not flag
            if (!getSetting().isEnabled() || equalsChunk || equalsPreviousChunk) {
                return Validation.PASS.copy().skip(skip).build();
            }
            return Validation.validate().flag().skip(skip).build();
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.d");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionD.class);
        }
    }

    public static class MovementE<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                return Validation.validate().cancel().flag().build();
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.E";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.e");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionE.class);
        }
    }

    public static class MovementF<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        private static final Map<Direction, Axis> axisLookup = new EnumMap<>(Direction.class);

        private enum Direction {
            UP, DOWN, NORTH, SOUTH, WEST, EAST, STILL
        }

        private enum Axis {
            X(Direction.WEST, Direction.EAST),
            Z(Direction.NORTH, Direction.SOUTH),
            Y(Direction.UP, Direction.DOWN);

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
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                final Location priorLocation = container.getStorage().getLocation(0);
                if (priorLocation == null) return Validation.PASS;

                final Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                final Direction[] priorVelocity = lastDirectionFlow;

                lastDirectionFlow = velocity;
                if (priorVelocity != null && priorVelocity.length == velocity.length) {

                    int i = 0;
                    final Validation builder = Validation.validate();
                    for (final Direction dir : velocity) {

                        final Axis axis = axisLookup.get(dir);
                        final Direction corr = priorVelocity[i++];

                        if (corr == Direction.STILL || dir == Direction.STILL) continue;

                        if (corr == axis.getOther(dir)) {
                            builder.add(Validation.validate().flag().withViolationKey(
                                    ViolationService.HoneyKey.of(axis.ordinal(), axis.name().toLowerCase())).build());
                        }
                    }
                    final ResponseContainer response;
                    if (!(response = builder.build()).subContainers.isEmpty())
                        return response;
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.F";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.f");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionF.class);
        }
    }

    public static class MovementG<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.G";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.g");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionG.class);
        }
    }

    public static class MovementH<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey twoDKey = ViolationService.HoneyKey.of(0, "2d");
        private static final ViolationService.HoneyKey YKey = ViolationService.HoneyKey.of(1, "y");

        private double lastDelta2D;
        private double lastDeltaY;

        @Override
        public synchronized ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            ResponseContainer response = Validation.PASS.copy().skip(1).build();
            if (getSetting().isEnabled()) {
                double delta2D = container.getStorage().getDouble(0);
                double deltaY = container.getStorage().getDouble(1);

                if (delta2D == 0 && deltaY == 0) {
                    return response;
                }

                boolean hasPattern = false;
                Validation builder = Validation.validate();

                if (delta2D > 0.05) {
                    if (lastDelta2D == delta2D) {
                        builder.add(
                                Validation.validate().flag()
                                        .withViolationKey(twoDKey)
                                        .build()
                        );
                        hasPattern = true;
                    }
                    lastDelta2D = delta2D;
                }

                if (deltaY > 0.5) {
                    Material material = player.player.getEyeLocation().getBlock().getType();
                    if (material.name().contains("WATER") || material.name().contains("LAVA") || deltaY % 0.375 < 0.01) {
                        lastDeltaY = deltaY;
                        if (hasPattern) {
                            return builder.build();
                        } else {
                            return response;
                        }
                    }

                    if (lastDeltaY == deltaY) {
                        builder.add(
                                Validation.validate().flag()
                                        .withViolationKey(YKey)
                                        .build()
                        );
                        hasPattern = true;
                    }
                    lastDeltaY = deltaY;
                }

                if (hasPattern) {
                    return builder.build();
                }
            }

            return response;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.H";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.h");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionH.class);
        }
    }

    public static class MovementI<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                return Validation.validate().flag().build();
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.I";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.i");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionI.class);
        }
    }

    public static class MovementJ<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final double x = packet.getX();
                final double y = packet.getY();
                final double z = packet.getZ();

                if (x > 3.0E7 || x < -3.0E7) {
                    return Validation.validate().cancel().flag().build();
                }
                if (z > 3.0E7 || z < -3.0E7) {
                    return Validation.validate().cancel().flag().build();
                }
                if (y < getSetting().min || y > getSetting().max) {
                    if (player.player.isDead())
                        return Validation.validate().cancel().build();

                    return Validation.validate().cancel().flag().onPunish(() -> {
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
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.J";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.j");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Position.PositionJ.class);
        }
    }
}
