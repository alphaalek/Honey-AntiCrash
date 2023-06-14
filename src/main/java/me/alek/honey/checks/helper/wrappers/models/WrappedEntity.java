package me.alek.honey.checks.helper.wrappers.models;

import me.alek.honey.checks.helper.wrappers.WrappedObject;
import me.alek.honey.checks.helper.converters.TwinConverter;
import org.bukkit.Location;

public class WrappedEntity extends WrappedObject<WrappedEntity> {

    private Location location;



    public Location getLocation() {
        return location;
    }

}
