package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.MovementPacketWrapper;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.validation.ViolationManagement;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import me.alek.honey.validation.*;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.function.Function;

public class MovementValidations<WP extends MovementPacketWrapper<WP>> extends AbstractValidationPacketListener<WP> {

    private static final Map<UUID, Location> lastTeleportationLocationMap = new HashMap<>();
    private static final Map<UUID, Location> lastPacketLocation = new HashMap<>();
    private static final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(null);

    public MovementValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
        HoneyPlugin.get().getPacketLibrary().addListeners(new PacketListenerAdapter<WrappedPlayOutPosition>() {

            @Override
            public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutPosition> packet) {
                lastTeleportationLocationMap.put(player.uuid, packet.getPlayer().getLocation());
            }

        }, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGHEST, Collections.singletonList(PacketTypes.Play.Server.PLAYER_POSITION));
    }

    private static List<PacketType> getPacketTypes() {
        return Arrays.asList(
                PacketTypes.Play.Client.PLAYER_POSITION,
                PacketTypes.Play.Client.PLAYER_POS_AND_ROT,
                PacketTypes.Play.Client.GROUND,
                PacketTypes.Play.Client.PLAYER_MOVEMENT
        );
    }

    public static String format(double value) {
        return String.valueOf(Math.ceil(value * 100) / 100);
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context, Callback callback) {
        Location currentLocation = packet.getAnyWrapped().getLocation();
        Location lastLocation = lastPacketLocation.getOrDefault(player.uuid, currentLocation);

        // check if the packet has any valid location to be checked, or just origin location
        // if it does, ignore this packet and avoid adding this location to the last packet location lookup
        if (LocationUtils.compareLocation(currentLocation, originLocation)) {
            return false;
        }
        lastPacketLocation.put(player.uuid, currentLocation);

        // check if the player has moved by checking if the last location is equal to the current location
        if (LocationUtils.compare3DLocation(currentLocation, lastLocation)) {
            return false;
        }

        // share this last location with the pipeline, so the checks can take use of it
        context.putLocation(lastLocation);

        callback.addAction(Callback.Action.<ViolationKey, ValidationServiceContext>from((keyOuter, response) -> {
            response.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) onViolation.run();

                final String x = format(currentLocation.getX());
                final String y = format(currentLocation.getY());
                final String z = format(currentLocation.getZ());

                context.getDevelopmentData(keyInner).addPoint("Position", x + ", " + y + ", " + z);
            });
        }));

        return true;
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new MovementA<>(), new MovementB<>(), new MovementC<>(), new MovementD<>(), new MovementE<>(),
                new MovementF<>(), new MovementG<>(), new MovementH<>(), new MovementI<>(), new MovementJ<>());
    }

    @Override
    public void cleanup(HoneyPlayer player) {
        lastPacketLocation.remove(player.uuid);
        lastTeleportationLocationMap.remove(player.uuid);
    }

    public static Location getLastLocationFor(HoneyPlayer player) {
        return lastPacketLocation.getOrDefault(player.uuid, player.getLocation());
    }

    public static boolean isChunkLoaded(Location location) {
        int x = location.getBlockX() >> 4;
        int z = location.getBlockZ() >> 4;

        return location.getWorld().isChunkLoaded(x, z);
    }

    public static boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordIntPair getChunkAt(Location location) {
        int x = ((int) location.getX()) >> 4;
        int z = ((int) location.getZ()) >> 4;

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

    public static class MovementA<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private boolean wasOnGround;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (packet.getType() != PacketTypes.Play.Client.PLAYER_MOVEMENT
                    && packet.getType() != PacketTypes.Play.Client.GROUND) {
                return ValidationServiceContext.PASS;
            }

            if (getSetting().isEnabled()) {
                boolean wasOnGround = this.wasOnGround;
                if ((this.wasOnGround = packet.onGround()) != wasOnGround) {
                    return ValidationServiceContext.Builder.validate().terminate().flag().build();
                }
            }

            return ValidationServiceContext.Builder.validate().terminate().build();
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementA.class);
        }
    }

    public static class MovementB<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationManagement.HoneyKey XZKey = ViolationManagement.HoneyKey.of(0, "2d");
        private static final ViolationManagement.HoneyKey YKey = ViolationManagement.HoneyKey.of(1, "y");

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            Location location = packet.getLocation();
            Location lastKnownLocation = context.getLocation(0);

            if (isAnyLatelyTeleportationLocationsCloseTo(
                    player, 15,
                    l -> LocationUtils.calculate3DDistance(l, location))) {
                return ValidationServiceContext.PASS.copy().skip(3).build();
            }

            double distance2d = LocationUtils.calculate2DDistance(location, lastKnownLocation);
            context.putDouble(distance2d);

            double distanceY = LocationUtils.calculateYDistance(location, lastKnownLocation);
            context.putDouble(distanceY);

            if (getSetting().isEnabled()) {
                ValidationServiceContext.Builder builder = ValidationServiceContext.Builder.validate();

                if (distance2d >= getSetting().max && distance2d != 0) {
                    builder.add(
                            ValidationServiceContext.Builder.validate().cancel().flag().withViolationKey(XZKey)
                                    .onViolation(ViolationKey.of(this, XZKey), (key, onViolation) -> {
                                        if (onViolation != null) onViolation.run();

                                        context.getDevelopmentData(key).addPoint("2D Distance", distance2d);
                                    })
                                    .build()
                    );
                }

                if (distanceY >= getSetting().max && distanceY != 0) {
                    builder.add(
                            ValidationServiceContext.Builder.validate().cancel().flag().withViolationKey(YKey)
                                    .onViolation(ViolationKey.of(this, YKey), (key, onViolation) -> {
                                        if (onViolation != null) onViolation.run();

                                        context.getDevelopmentData(key).addPoint("Y Distance", distanceY);
                                    })
                                    .build()
                    );
                }

                if (!builder.build().subContainers.isEmpty()) {
                    return builder.build();
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementB.class);
        }
    }

    public static class MovementC<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private Location lastFlaggedLocation;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                // ignores when you fall down. note, that if it would be at 0, it will also ignore when you
                // walk around on ground, because the gravity affect the player's velocity
                if (player.bukkit.getVelocity().getY() < -1.5) {
                    return ValidationServiceContext.PASS;
                }

                Location priorLocation = context.getLocation(0);
                // return if it's the first location being sent
                if (priorLocation == null) {
                    return ValidationServiceContext.PASS;
                }

                // ignores packets close to the recent flag
                if (lastFlaggedLocation != null
                        && LocationUtils.calculate3DDistance(lastFlaggedLocation, packet.getLocation()) < 1) {
                    return ValidationServiceContext.PASS;
                }
                lastFlaggedLocation = packet.getLocation();

                double priorDistance = LocationUtils.calculate3DDistance(priorLocation, packet.getLocation());
                return ValidationServiceContext.Builder.validate().flag(Math.min(10, priorDistance)).build();
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementC.class);
        }
    }

    public static class MovementD<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private WrappedChunkCoordIntPair lastChunkCameFrom;
        private WrappedChunkCoordIntPair lastChunk;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
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
            // the skip is for letting the next check in the check pipeline (Movement.E), either flag or not flag
            if (!getSetting().isEnabled()
                    || equalsChunk
                    || equalsPreviousChunk
                    || lastChunkCameFrom == null) {
                return ValidationServiceContext.PASS.copy().skip(skip).build();
            }

            return ValidationServiceContext.Builder.validate().flag().skip(skip).build();
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementD.class);
        }
    }

    public static class MovementE<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag().build();
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementE.class);
        }
    }

    public static class MovementF<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

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
            private final ViolationManagement.HoneyKey key = ViolationManagement.HoneyKey.of(ordinal(), name().toLowerCase());

            public Direction getOther(Direction dir) {
                return (dir == opp1) ? opp2 : opp1;
            }

            public ViolationManagement.HoneyKey getKey() {
                return key;
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
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                Location priorLocation = context.getLocation(0);
                if (priorLocation == null) {
                    return ValidationServiceContext.PASS;
                }

                Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                Direction[] priorVelocity = lastDirectionFlow;
                lastDirectionFlow = velocity;
                if (priorVelocity == null) {
                    return ValidationServiceContext.PASS;
                }

                ValidationServiceContext.Builder builder = ValidationServiceContext.Builder.validate();
                for (int i = 0; i < 3; i++) {
                    Direction dir = velocity[i];
                    Direction corr = priorVelocity[i];
                    if (corr == Direction.STILL || dir == Direction.STILL) {
                        continue;
                    }

                    Axis axis = axisLookup.get(dir);
                    if (corr.equals(axis.getOther(dir))) {
                        builder.add(
                                ValidationServiceContext.Builder.validate().flag()
                                        .withViolationKey(axis.getKey())
                                        .build()
                        );
                    }
                }

                if (!builder.build().subContainers.isEmpty()) {
                    return builder.build();
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementF.class);
        }
    }

    public static class MovementG<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementG.class);
        }
    }

    public static class MovementH<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP>{

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationManagement.HoneyKey twoDKey = ViolationManagement.HoneyKey.of(0, "2d");
        private static final ViolationManagement.HoneyKey YKey = ViolationManagement.HoneyKey.of(1, "y");

        private double lastDelta2D;
        private double lastDeltaY;

        @Override
        public synchronized ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                double delta2D = context.getDouble(0);
                double deltaY = context.getDouble(1);
                if (delta2D == 0 && deltaY == 0) {
                    return ValidationServiceContext.PASS;
                }
                if (player.isInLiquid() || deltaY % 0.375 < Math.pow(10, -3)) {
                    return ValidationServiceContext.PASS;
                }

                ValidationServiceContext.Builder builder = ValidationServiceContext.Builder.validate();

                if (lastDeltaY == deltaY && deltaY > 0) {
                    builder.add(
                            context.put(ValidationServiceContext.class,
                                    ValidationServiceContext.Builder.validate().flag()
                                            .withViolationKey(YKey)
                                            .build()
                            )
                    );
                }
                if (lastDelta2D == delta2D && delta2D > 0) {
                    builder.add(
                            context.put(ValidationServiceContext.class,
                                    ValidationServiceContext.Builder.validate().flag()
                                            .withViolationKey(twoDKey)
                                            .build()
                            )
                    );
                }

                lastDeltaY = deltaY;
                lastDelta2D = delta2D;

                return builder.build();
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementH.class);
        }
    }

    public static class MovementI<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                ValidationServiceContext.Builder builder = ValidationServiceContext.Builder.validate();
                int i = 0;
                for (;;) {
                    ValidationServiceContext flagContext = context.get(ValidationServiceContext.class, i++);
                    if (flagContext != null) {
                        builder.add(flagContext);
                        continue;
                    }

                    if (builder.build().subContainers.isEmpty()) {
                        return ValidationServiceContext.PASS;
                    }

                    return builder.build();
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementI.class);
        }
    }

    public static class MovementJ<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                final double x = packet.getX();
                final double y = packet.getY();
                final double z = packet.getZ();

                if (x > 3.0E7
                        || x < -3.0E7
                        || z > 3.0E7
                        || z < -3.0E7) {
                    return ValidationServiceContext.Builder.validate().cancel().flag().build();
                }

                if (y < getSetting().min || y > getSetting().max) {
                    if (player.bukkit.isDead()) {
                        return ValidationServiceContext.Builder.validate().cancel().build();
                    }

                    return ValidationServiceContext.Builder.validate().cancel().flag().onPunish(() -> {
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
                        player.bukkit.teleport(new Location(player.getWorld(), x, yPosFix, z));
                    }).build();
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementJ.class);
        }
    }
}
