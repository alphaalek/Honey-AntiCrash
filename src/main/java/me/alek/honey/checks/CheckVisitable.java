package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;

public interface CheckVisitable {

    CheckVisitor<?> getVisitor(HoneyPlayer player);

    void setInitializedCheckInstances();

}
