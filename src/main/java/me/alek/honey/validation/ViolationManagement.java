package me.alek.honey.validation;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.util.Array;
import me.alek.honey.util.discord.DiscordWebhook;
import me.alek.honey.util.statics.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ViolationManagement {

    private static ViolationManagement INSTANCE;

    public ViolationManagement() {
        INSTANCE = this;
    }

    public void reload() {
        // loop through all the players and update their packet limit handlers
        for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
            player.violationContext = new ViolationContext(player);
        }
    }

    public ViolationDistributor getDistributor(HoneyPlayer player, HoneyKey violationKey, ConfigurationOptions.ViolationSetting setting) {
        return player.violationContext.getDistributor(violationKey, setting);
    }

    public boolean flag(HoneyPlayer player,
                        HoneyValidation<?> validation,
                        ConfigurationOptions.ViolationSetting setting,
                        ValidationPipelineContext.DevelopmentData developmentData
    ) {
        return flag(player, Cancellable.Trigger.create(), validation.generateKey(), null, 1, null, null, validation, setting, developmentData);
    }

    public boolean flag(HoneyPlayer player,
                        Cancellable cancellable,
                        HoneyKey distributionKey,
                        HoneyKey violationKey,
                        double amount,
                        Runnable onPunish,
                        Runnable onViolation,
                        HoneyValidation<?> validation,
                        ConfigurationOptions.ViolationSetting setting,
                        ValidationPipelineContext.DevelopmentData developmentData
    ) {
        return getDistributor(player, distributionKey, setting)
                .flag(violationKey, cancellable, validation, amount, onPunish, onViolation, developmentData);
    }

    public synchronized void punish(Runnable onPunish, HoneyPlayer player, HoneyValidation<?> validation, HoneyKey violationKey) {
        if (onPunish != null) {
            onPunish.run();
        }
        if (player.punished) {
            return;
        }
        player.punished = true;

        player.channel.eventLoop().execute(() -> ChannelCloseInspection.close(player.channel));

        if (player.linkedBukkit() && player.isOnlineBukkit()) {
            player.kick();
        }

        if (validation != null && player.linkedBukkit()) {
            HoneyPlugin.get().getNotificationService().warn(player, validation.getName(), violationKey);
        }
    }

    public static class ViolationContext {

        private final HoneyPlayer player;
        private final Array<ViolationDistributor> distributors = new Array<>();

        public ViolationContext(HoneyPlayer player) {
            this.player = player;
        }

        public ViolationDistributor getDistributor(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            return distributors.get(key.getRegistryId(), () -> {
                if (setting.keyedAnnotation != null) {
                    return new ViolationDistributor(player,
                            new KeyedViolationLookupGenerator(key, setting));
                } else {
                    AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), this.player, setting);
                    if (packetLimitHandler == null) {
                        throw new RuntimeException("No packet limit handler could be created: " + setting.getPacketLimitHandlerType());
                    }

                    return new ViolationDistributor(player, packetLimitHandler);
                }
            });
        }

        public void setSetting(HoneyKey key, HoneyKey violationKey, ConfigurationOptions.ViolationSetting setting) {
            if (distributors.has(key.getRegistryId())) {
                distributors.get(key.getRegistryId()).setSetting(violationKey, setting);
            }
        }

        public Array<ViolationDistributor> getViolationDistributors() {
            return distributors;
        }

        public ViolationDistributor getHandler(HoneyKey key) {
            return distributors.get(key.getRegistryId(), null);
        }

        public HoneyPlayer getPlayer() {
            return player;
        }
    }

    private static AbstractPacketLimitHandler generatePacketLimitHandler(PacketLimitHandlerType type, HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
        switch (type) {
            case MIN:
            case MAX:
            case SIMPLE:
                return new PacketSimpleHandler(player, setting);
            case INTERVAL:
                return new PacketIntervalHandler(player, setting);
            case INTERVAL_THROTTLE:
                return new PacketIntervalThrottleHandler(player, setting);
            case SINGLE_OVERFLOW:
                return new PacketSingleOverflowHandler(player, setting);
            case BUFFER_OVERFLOW:
                return new PacketBufferOverflowHandler(player, setting);
            case BUFFER_THROTTLE:
                return new PacketBufferThrottleHandler(player, setting);
            case TRIGGER_THROTTLE:
                return new PacketTriggerThrottleHandler(player, setting);
            case CHIPSTATE_THROTTLE:
                return new PacketChipstateThrottleHandler(player, setting);
            default:
                return null;
        }
    }

    public static class KeyedViolationLookupGenerator {

        private final ConfigurationOptions.ViolationSetting setting;
        private final HoneyKey limitsKey;

        public KeyedViolationLookupGenerator(HoneyKey limitsKey, ConfigurationOptions.ViolationSetting setting) {
            this.setting = setting;
            this.limitsKey = limitsKey;
        }

        public Map<HoneyKey, AbstractPacketLimitHandler> generateMap(HoneyPlayer player) {
            if (setting.keyedAnnotation == null || setting.keyedAnnotation.amount() <= 0) {
                return new HashMap<HoneyKey, AbstractPacketLimitHandler>() {{
                    put(HoneyKey.of(limitsKey.getRegistryId()), generateHandler(player));
                }};
            }

            final Map<HoneyKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (int i = 0; i < setting.keyedAnnotation.amount(); i++) {
                map.put(HoneyKey.of(i), generateHandler(player));
            }

            return map;
        }

        private AbstractPacketLimitHandler generateHandler(HoneyPlayer player) {
            return generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
        }
    }

    public static class ViolationDistributor {

        private final HoneyPlayer player;
        private final Array<AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new Array<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(HoneyPlayer player, KeyedViolationLookupGenerator generator) {
            this.player = player;

            for (Map.Entry<HoneyKey, AbstractPacketLimitHandler> entry : generator.generateMap(player).entrySet()) {
                packetLimitHandlerDelegateLookup.set(entry.getKey().getRegistryId(), entry.getValue());
            }
        }

        public ViolationDistributor(HoneyPlayer player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.set(0, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) {
                return null;
            }

            return Arrays.stream(
                            packetLimitHandlerDelegateLookup
                                    .compact()
                                    .getHandle(AbstractPacketLimitHandler[]::new))
                    .findAny()
                    .orElse(null);
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(HoneyKey key) {
            return packetLimitHandlerDelegateLookup.get(key.getRegistryId());
        }

        public Array<AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        private AbstractPacketLimitHandler getHandler(HoneyKey key) {
            if (packetLimitHandlerDelegateLookup.isEmpty()
                    && cachedDefaultPacketLimitHandler == null) {
                return null;
            }

            if (key == null) {
                return cachedDefaultPacketLimitHandler == null
                        ? (cachedDefaultPacketLimitHandler = getAnyPacketLimitHandler())
                        : cachedDefaultPacketLimitHandler;
            }

            return getPacketLimitHandler(key);
        }

        public void createHandler(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            if (packetLimitHandlerDelegateLookup.has(key)) {
                return;
            }

            AbstractPacketLimitHandler handler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
            packetLimitHandlerDelegateLookup.set(key, handler);
        }

        public void setSetting(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return;
            }

            handler.setting = setting;
        }

        public boolean flag(HoneyKey key, Cancellable cancellable,
                            HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation,
                            ValidationPipelineContext.DevelopmentData developmentData) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return false;
            }

            return handler.flag(cancellable, validation, amount, onPunish, onViolation, developmentData, key);
        }
    }

    public static abstract class AbstractPacketLimitHandler {

        private final HoneyPlayer player;
        private ConfigurationOptions.ViolationSetting setting;

        private long lastViolationReset;
        private int vls;

        public AbstractPacketLimitHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            this.player = player;
            this.setting = setting;

            this.lastViolationReset = System.currentTimeMillis();
        }

        public boolean violate(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (System.currentTimeMillis() - this.lastViolationReset >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                this.vls = 0;
                this.lastViolationReset = System.currentTimeMillis();
            }

            if (onViolation != null) {
                onViolation.run();
            }
            cancellable.cancel();

            if (++this.vls >= this.setting.minVls) {
                doFormatStuff(validation, developmentData, violationKey);
            }

            if (this.vls < this.setting.vlsAmount) {
                return false;
            }

            INSTANCE.punish(onPunish, this.player, validation, violationKey);
            return true;
        }

        public void doFormatStuff(HoneyValidation<?> validation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (developmentData != null) {
                HoneyPlugin.get().getNotificationService().applyQuotients(this.player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            HoneyPlugin.get().getNotificationService().notify(
                    this.player, validation.getName(), violationKey, getSetting(), this, developmentData);

            if (DiscordWebhook.enabled()) {
                DiscordWebhook.postViolation(
                        this.player, validation,
                        ChatColor.stripColor(AbstractPacketLimitHandler.this.format()),
                        developmentData);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        HoneyPlugin.get().getSQLManager().getComponentBridge().postViolation(player,
                                validation.getName() + (violationKey != null && violationKey.getIdentifier() != null ? " (" + violationKey.getIdentifier() + ")" : ""),
                                ChatColor.stripColor(AbstractPacketLimitHandler.this.format()), developmentData);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(HoneyPlugin.get().getPlugin());
        }

        private void applySettingsToDevelopmentData(ValidationPipelineContext.DevelopmentData developmentData) {
            developmentData.addEmpty();

            PacketLimitHandlerType handlerType = this.setting.getPacketLimitHandlerType();
            developmentData.addHeader("Settings", (handlerType == PacketLimitHandlerType.SIMPLE ? null : this.setting.getPacketLimitHandlerType().name().toLowerCase().replace("_", " ")));

            for (Map.Entry<String, Object> valueEntry : this.setting.getValues()) {
                developmentData.addLine(valueEntry.getKey() + ": " + valueEntry.getValue());
            }
        }

        public String format() {
            return "§7flags: §e" + getViolations() + "§7vls/§e" + this.setting.vlsAmount + "§7vls";
        }

        public int getViolations() {
            return this.vls;
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public ConfigurationOptions.ViolationSetting getSetting() {
            return setting;
        }

        public abstract boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey);

        public abstract PacketLimitHandlerType getType();
    }

    public static class PacketChipstateThrottleHandler extends AbstractPacketLimitHandler {

        public PacketChipstateThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (amount == 1) {
                cancellable.cancel();
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.CHIPSTATE_THROTTLE;
        }
    }

    public static class PacketTriggerThrottleHandler extends AbstractPacketLimitHandler {

        private boolean notFlaggedLast;
        private boolean flagged;
        private double lastFlag;

        public PacketTriggerThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (amount < 1) {
                notFlaggedLast = true;
                flagged = false;
            }

            if (amount >= 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (amount >= 1) {
                    if (notFlaggedLast && !flagged) {
                        violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
                        flagged = true;
                    }

                    lastFlag = System.currentTimeMillis();
                    notFlaggedLast = true;
                }

                cancellable.cancel();
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.TRIGGER_THROTTLE;
        }
    }

    public static class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public static class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private double amount;

        public PacketSingleOverflowHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public String format() {
            return "§7flags: §e" + NumberUtils.format(amount, 2) + "§7/§e" + getSetting().threshold + "§7, §e" + getViolations() + "§7vls/§e" + getSetting().vlsAmount + "§7vls";
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if ((this.amount = amount) < getSetting().threshold) return false;

            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SINGLE_OVERFLOW;
        }
    }

    public static abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double buffer;


        public PacketBufferHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            this.buffer -= getSetting().decayAmount;
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            this.hasDecayed = false;
            long diff = System.currentTimeMillis() - this.lastDecay;
            while (this.buffer > 0 && (diff -= getSetting().decay) > 0) {
                this.hasDecayed = true;
                this.lastDecay = System.currentTimeMillis();
                decay();
            }

            if (this.buffer < 0) {
                this.buffer = 0;
            }
            this.buffer += amount;

            if (this.buffer < getSetting().threshold) {
                return false;
            }

            return handle(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
        }

        public abstract boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey);
    }

    public static class PacketBufferThrottleHandler extends PacketBufferHandler {

        public PacketBufferThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public String format() {
            return "§7buffer: §e" + NumberUtils.format(this.buffer, 2) + "§7/§e" + getSetting().until + "§7/§e" + getSetting().threshold + "§7, §e"
                    + getViolations() + "§7vls/§e" + getSetting().vlsAmount + "§7vls";
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_THROTTLE;
        }

        @Override
        public void decay() {
            if (this.buffer < getSetting().threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((this.buffer - getSetting().threshold) / (getSetting().decayAmount * 1.8));
            double amount = getSetting().decayAmount * Math.pow(1.4, ratio);

            this.buffer -= amount;
        }

        @Override
        public boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            cancellable.cancel();

            if (this.buffer > getSetting().until && getSetting().isEnabled()) {
                violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            }

            return true;
        }
    }

    public static class PacketBufferOverflowHandler extends PacketBufferHandler {

        public PacketBufferOverflowHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public String format() {
            return "§7buffer: §e" + NumberUtils.format(this.buffer, 2) + "§7/§e" + getSetting().threshold +
                    "§7, §e" + getViolations() + "§7vls/§e" + getSetting().vlsAmount + "§7vls";
        }

        @Override
        public boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public static class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private boolean violatedForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        public double flaggedCopy;

        public PacketIntervalHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public String format() {
            return "§7flags: §e" + NumberUtils.format(this.flaggedCopy, 2) + "§7/§e" + getSetting().maxPerInterval + "§7, §e" + getViolations() + "§7vls/§e" + getSetting().vlsAmount + "§7vls, §e"
                    + (getSetting().interval < 1000 ? getSetting().interval + "§7ms" : ((double) getSetting().interval) / 1000 + "§7s");
        }

        private synchronized void clearInterval() {
            this.violatedForInterval = false;
            this.flagged = 0;
            this.intervalStart = System.currentTimeMillis();
        }

        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (System.currentTimeMillis() - this.intervalStart >= getSetting().interval) {
                clearInterval();
            }

            this.flagged += amount;
            if (this.flagged < getSetting().maxPerInterval) {
                return false;
            }

            cancellable.cancel();

            if (this.violatedForInterval) {
                return true;
            }

            this.violatedForInterval = true;
            this.flaggedCopy = flagged;
            clearInterval();

            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public static class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public String format() {
            return "§7flags: §e" + amount + "§7/§e" + getSetting().threshold + "§7/§e"
                    + getSetting().until + "§7, §e" + getViolations() + "§7vls/§e" + getSetting().vlsAmount + "§7vls";
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > getSetting().interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                } else {
                    cancellable.cancel();
                }
            }

            if ((this.amount = amount) > getSetting().maxPerInterval) {
                intervalStart = System.currentTimeMillis();

                if (amount > getSetting().until && !currentlyCancelling) {
                    violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
                }
                currentlyCancelling = true;
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL_THROTTLE;
        }
    }
}
