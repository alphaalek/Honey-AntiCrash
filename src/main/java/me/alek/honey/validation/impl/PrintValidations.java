package me.alek.honey.validation.impl;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.Settings;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ViolationValidation;

public class PrintValidations {

    public static final ValidationPipeline<ViolationValidation.IntViolation> PIPELINE = ValidationPipeline.from(new PrintA(), new PrintB());

    public static class PrintA implements ViolationValidation.IntViolation {
        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Print.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.print.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Print.PrintA.class);
        }
    }

    public static class PrintB implements ViolationValidation.IntViolation {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Print.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.print.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Print.PrintB.class);
        }
    }
}
