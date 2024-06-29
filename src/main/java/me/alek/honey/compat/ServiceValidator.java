package me.alek.honey.compat;

import me.alek.honey.util.LazyInit;

public interface ServiceValidator {

    LazyInit<Boolean> find();
}
