package me.alek.honey.netty.structure.converters;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;

public class SimpleWrapperConverter<WO extends WrappedObject<WO>> implements JavaConverter<Object, WO> {

    protected final ConstructorInvoker<WO> constructorInvoker;

    public SimpleWrapperConverter(Class<WO> clazz, Class<?>... params) {
        Class<?>[] paramsFixed = new Class[params.length + 1];
        paramsFixed[0] = Object.class;
        for (int i = 0; i < params.length; i++) {
            paramsFixed[i + 1] = params[i].getClass();
        }
        this.constructorInvoker = new ConstructorInvoker<>(clazz, paramsFixed);
    }

    @Override
    public WO convertDelegate(Object delegate, Object... params) {
        return constructorInvoker.invoke(delegate);
    }

    @Override
    public Object convertDeclaring(WO object) {
        return object.getHandle();
    }
}
