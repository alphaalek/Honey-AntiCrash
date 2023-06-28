package me.alek.honey.netty.structure.converters;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;

public class SimpleWrapperConverter<WO extends WrappedObject<WO>> implements JavaConverter<Object, WO> {

    private final ConstructorInvoker<WO> constructorInvoker;

    public SimpleWrapperConverter(Class<WO> clazz) {
        this.constructorInvoker = new ConstructorInvoker<>(clazz, Object.class);
    }

    @Override
    public WO convertDelegate(Object delegate) {
        return constructorInvoker.invoke(delegate);
    }

    @Override
    public Object convertDeclaring(WO object) {
        return object.getHandle();
    }
}
