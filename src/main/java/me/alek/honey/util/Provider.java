package me.alek.honey.util;

public interface Provider<T, P extends Provider<T, P, A>, A extends RemoteProviderBridge<T, P, A>> {

    A createRemoveBridge();

    interface ProviderNoInput<T> extends Provider<T, ProviderNoInput<T>, RemoteProviderBridge.ProviderNoInput<T>> {

        T get();

        default RemoteProviderBridge.ProviderNoInput<T> createRemoveBridge() {
            return (RemoteProviderBridge.ProviderNoInput<T>) RemoteProviderBridge.<T>getWithNoInput();
        }
    }

    interface ProviderOneInput<T, I1> extends Provider<T, ProviderOneInput<T, I1>, RemoteProviderBridge.ProviderOneInput<T, I1>> {

        T get(I1 i1);

        default RemoteProviderBridge.ProviderOneInput<T, I1> createRemoveBridge() {
            return (RemoteProviderBridge.ProviderOneInput<T, I1>) RemoteProviderBridge.<T, I1>getWithOneInput();
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends Provider<T, ProviderTwoInput<T, I1, I2>, RemoteProviderBridge.ProviderTwoInput<T, I1, I2>> {

        T get(I1 i1, I2 i2);

        default RemoteProviderBridge.ProviderTwoInput<T, I1, I2> createRemoveBridge() {
            return (RemoteProviderBridge.ProviderTwoInput<T, I1, I2>) RemoteProviderBridge.<T, I1, I2>getWithTwoInput();
        }
    }

    interface ProviderGenericInput<T> extends Provider<T, ProviderGenericInput<T>, RemoteProviderBridge.ProviderGenericInput<T>> {

        T get(Object[] array);

        default RemoteProviderBridge.ProviderGenericInput<T> createRemoveBridge() {
            return (RemoteProviderBridge.ProviderGenericInput<T>) RemoteProviderBridge.<T>getWithGenericInput();
        }
    }
}
