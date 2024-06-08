package me.alek.honey.netty.structure.converters;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.util.ConstructorInvoker;
import org.bukkit.Bukkit;

public class SimpleWrapperConverter<WO extends WrappedObject<WO>> implements JavaConverter<Object, WO> {

    protected final ConstructorInvoker<WO> constructorInvoker;

    public SimpleWrapperConverter(Class<WO> clazz, Class<?>... params) {
        Class<?>[] paramsFixed = new Class[params.length + 1];
        paramsFixed[0] = Object.class;
        System.arraycopy(params, 0, paramsFixed, 1, params.length);
        this.constructorInvoker = new ConstructorInvoker<>(clazz, paramsFixed);
    }

    @Override
    public WO convertDelegate(Object delegate, Object... params) {
        if (params.length == 0) {
            return constructorInvoker.invoke(delegate);
        } else {
            return constructorInvoker.invokeAndFixArrays(delegate, params);
        }
    }

    @Override
    public Object convertDeclaring(WO object) {
        return object.getHandle();
    }
}
