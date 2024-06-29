package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ValidationPipeline<C extends HoneyValidation<?>> {

    private final List<C> validations = new LinkedList<>();

    public static <C extends HoneyValidation<?>> ValidationPipeline<C> from(C... validations) {
        ValidationPipeline<C> pipeline = new ValidationPipeline<>();
        Arrays.stream(validations).forEach(pipeline::register);

        return pipeline;
    }

    public void register(C validation) {
        validations.add(validation);
    }

    private ValidationServiceContext getResponseAndRegisterTimings(C validation, Function<C, ValidationServiceContext> getResponse, ConfigurationOptions.VlSetting setting) {
        long registryKey = -1;
        if (setting != null && setting.isEnabled()) {
            registryKey = HoneyPlugin.get().getValidationHandleTracker().registerTimeStart(validation.getName());
        }

        ValidationServiceContext response = getResponse.apply(validation);

        if (setting != null && setting.isEnabled() && registryKey != -1) {
            HoneyPlugin.get().getValidationHandleTracker().registerTimeStop(validation.getName(), registryKey);
        }

        return response;
    }

    private void clearPosts(ValidationServiceContext context) {
        context.onViolation = null;
        context.onPunish = null;
    }

    public void doStream(HoneyPlayer player, BiFunction<C, ValidationPipelineContext, ValidationServiceContext> getResponse, Cancellable cancellable) {
        ValidationPipelineContext container = new ValidationPipelineContext();
        doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                (c) -> getResponse.apply(c, container), cancellable, container, new Callback());
    }

    public void doStream(HoneyPlayer player,
                         BiFunction<C, ViolationService.HoneyKey, ConfigurationOptions.VlSetting> getSetting,
                         Function<C, ValidationServiceContext> getResponse,
                         Cancellable cancellable, ValidationPipelineContext pipelineContext, Callback callback) {
        int skipping = 0;
        for (C validation : validations) {
            if (skipping-- > 0) {
                continue;
            }

            // ---------------------
            // pre requisites
            ViolationService.HoneyKey distributionKey = ViolationService.HoneyKey.of(validation);
            ConfigurationOptions.VlSetting setting = getSetting.apply(validation, distributionKey);

            // start to check this validation
            ValidationServiceContext defaultResponse = getResponseAndRegisterTimings(validation, getResponse, setting);
            List<ValidationServiceContext> responsesList = new LinkedList<>(defaultResponse.subContainers);
            responsesList.add(defaultResponse);

            // handle all the responses
            boolean terminated = false;
            for (ValidationServiceContext context : responsesList) {
                if (context.skipIndices != 0) {
                    skipping = context.skipIndices;
                }
                if (context.success) {
                    if (context.terminate) {
                        terminated = true;
                    }
                    clearPosts(context);
                    continue;
                }
                if (context.cancelled) {
                    cancellable.cancel();
                }
                if (context.flag != -1 && player.isBukkitReady()) {
                    ViolationKey violationKey = new ViolationKey(validation, context.violationKey);
                    ValidationPipelineContext.DevelopmentData developmentData = pipelineContext.getDevelopmentData(violationKey);

                    if (validation.isDefaultPipeline()) {
                        callback.call(violationKey, context);
                        context.builder().onViolation(violationKey, (key, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + validation.getName() + " §8*");
                            developmentData.setLine(6, validation.getDescription());
                        });
                    }

                    Consumer<ViolationKey> onViolation = context.onViolation;
                    HoneyPlugin.get().getViolationService().flag(
                            player, cancellable, distributionKey, context.violationKey, context.flag, context.onPunish,
                            Optional.ofNullable(onViolation).map(o -> (Runnable) () -> o.accept(violationKey)).orElse(null),
                            validation, setting, developmentData);
                }
                clearPosts(context);

                if (player.punished) {
                    cancellable.cancel();
                    return;
                }

                if (context.terminate) {
                    terminated = true;
                }
            }

            if (terminated) {
                return;
            }
        }
    }
}
