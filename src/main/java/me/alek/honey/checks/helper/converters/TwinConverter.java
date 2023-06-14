package me.alek.honey.checks.helper.converters;

import me.alek.honey.netty.utility.reflect.ConstructorInvoker;

public interface TwinConverter<T, C> {

    C convertDelegate(T delegate);

    T convertDeclaring(C object);

    ConstructorInvoker getDelegatedConstructor();

}
