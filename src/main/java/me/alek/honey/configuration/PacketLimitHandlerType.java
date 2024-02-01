package me.alek.honey.configuration;

import com.google.common.collect.Sets;

import java.util.Set;

public enum PacketLimitHandlerType {

    TRIGGER_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.THROTTLE_AFTER,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    INTERVAL(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.INTERVAL,
            ConfigurationOptions.VlSettingImplType.MAX_PER_INTERVAL,
            ConfigurationOptions.VlSettingImplType.CLEAR_INTERVAL,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    INTERVAL_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.INTERVAL,
            ConfigurationOptions.VlSettingImplType.THRESHOLD,
            ConfigurationOptions.VlSettingImplType.UNTIL,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    SINGLE_OVERFLOW(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.THRESHOLD,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    BUFFER_OVERFLOW(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.DECAY,
            ConfigurationOptions.VlSettingImplType.DECAY_AMOUNT,
            ConfigurationOptions.VlSettingImplType.THRESHOLD,
            ConfigurationOptions.VlSettingImplType.CLEAR_BUFFER,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    BUFFER_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.DECAY,
            ConfigurationOptions.VlSettingImplType.DECAY_AMOUNT,
            ConfigurationOptions.VlSettingImplType.THRESHOLD,
            ConfigurationOptions.VlSettingImplType.UNTIL,
            ConfigurationOptions.VlSettingImplType.CLEAR_BUFFER,
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    )),

    SIMPLE(Sets.newHashSet(
            ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
    ));

    private final Set<ConfigurationOptions.VlSettingImplType> vlSettingImplTypes;

    PacketLimitHandlerType(Set<ConfigurationOptions.VlSettingImplType> vlSettingImplTypes) {
        this.vlSettingImplTypes = vlSettingImplTypes;
    }

    public Set<ConfigurationOptions.VlSettingImplType> getVlSettingImplTypes() {
        return vlSettingImplTypes;
    }

}
