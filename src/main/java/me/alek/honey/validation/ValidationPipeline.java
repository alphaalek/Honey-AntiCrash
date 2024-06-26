package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
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

    private ResponseContainer getResponseAndRegisterTimings(C validation, Function<C, ResponseContainer> getResponse, ConfigurationOptions.VlSetting setting) {
        long registryKey = -1;
        if (setting != null && setting.isEnabled()) {
            registryKey = HoneyPlugin.get().getValidationHandleTracker().registerTimeStart(validation.getName());
        }

        ResponseContainer response = getResponse.apply(validation);

        if (setting != null && setting.isEnabled() && registryKey != -1) {
            HoneyPlugin.get().getValidationHandleTracker().registerTimeStop(validation.getName(), registryKey);
        }

        return response;
    }

    public void doStream(HoneyPlayer player, BiFunction<C, ValidationContextContainer, ResponseContainer> getResponse, Cancellable cancellable) {
        ValidationContextContainer container = new ValidationContextContainer();
        doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                (c) -> getResponse.apply(c, container), cancellable, container, new CallbackAction<>());
    }

    public void doStream(HoneyPlayer player,
                         BiFunction<C, ViolationService.HoneyKey, ConfigurationOptions.VlSetting> getSetting,
                         Function<C, ResponseContainer> getResponse,
                         Cancellable cancellable, ValidationContextContainer container, CallbackAction<ResponseContainer> callback) {
        int skipping = 0;
        for (C validation : validations) {
            if (skipping-- > 0) {
                continue;
            }

            // ---------------------
            // pre requisites
            ViolationService.HoneyKey key = ViolationService.HoneyKey.of(validation);
            ConfigurationOptions.VlSetting setting = getSetting.apply(validation, key);
            // ---------------------

            // start to check this validation
            ResponseContainer defaultResponse = getResponseAndRegisterTimings(validation, getResponse, setting);
            List<ResponseContainer> responsesList = new LinkedList<>(defaultResponse.subContainers);
            responsesList.add(defaultResponse);

            // handle all the responses
            boolean terminated = false;
            ValidationContextContainer.DevelopmentData developmentData = container.getDevelopmentData(validation);
            for (ResponseContainer response : responsesList) {
                if (response == null) {
                    continue;
                }
                if (response.skipIndices != 0) {
                    skipping = response.skipIndices;
                }
                if (response.success) {
                    if (response.terminate) {
                        terminated = true;
                    }
                    continue;
                }
                if (response.cancelled) {
                    cancellable.cancel();
                }
                if (response.flag != -1 && player.isBukkitReady()) {
                    if (setting == null) {
                        if (response.terminate) {
                            terminated = true;
                        }
                        continue;
                    }

                    callback.call(response);
                    response.builder().onViolation((c, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + validation.getName() + " §8*");
                        developmentData.setLine(5, validation.getDescription());
                    });

                    HoneyPlugin.get().getViolationService().flag(
                            player, cancellable, key, response.violationKey, response.flag,
                            response.onPunish, () -> response.onViolation.accept(validation), response.onFlag, validation, setting, developmentData);

                    if (player.punished) {
                        cancellable.cancel();
                        return;
                    }
                }

                if (response.punish) {
                    HoneyPlugin.get().getViolationService().punish(null, player, validation, key);
                    return;
                }

                if (response.terminate) {
                    terminated = true;
                }
            }

            if (terminated) {
                return;
            }
        }
    }
}
