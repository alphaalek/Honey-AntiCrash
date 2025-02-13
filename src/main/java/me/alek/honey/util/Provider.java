package me.alek.honey.util;

public interface Provider<T, P extends Provider<T, P, A>, A extends RemoteProviderBridge<T, P, A>> {

    A createRemoteBridge();

    interface ProviderNoInput<T> extends Provider<T, ProviderNoInput<T>, RemoteProviderBridge.ProviderNoInput<T>> {

        T get();

        default RemoteProviderBridge.ProviderNoInput<T> createRemoteBridge() {
            return (RemoteProviderBridge.ProviderNoInput<T>) RemoteProviderBridge.Factory.<T>createWithNoInput();
        }
    }

    interface ProviderOneInput<T, I1> extends Provider<T, ProviderOneInput<T, I1>, RemoteProviderBridge.ProviderOneInput<T, I1>> {

        T get(I1 i1);

        default RemoteProviderBridge.ProviderOneInput<T, I1> createRemoteBridge() {
            return (RemoteProviderBridge.ProviderOneInput<T, I1>) RemoteProviderBridge.Factory.<T, I1>createWithOneInput();
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends Provider<T, ProviderTwoInput<T, I1, I2>, RemoteProviderBridge.ProviderTwoInput<T, I1, I2>> {

        T get(I1 i1, I2 i2);

        default RemoteProviderBridge.ProviderTwoInput<T, I1, I2> createRemoteBridge() {
            return (RemoteProviderBridge.ProviderTwoInput<T, I1, I2>) RemoteProviderBridge.Factory.<T, I1, I2>createWithTwoInput();
        }
    }

    interface ProviderGenericInput<T> extends Provider<T, ProviderGenericInput<T>, RemoteProviderBridge.ProviderGenericInput<T>> {

        T get(Object[] array);

        default RemoteProviderBridge.ProviderGenericInput<T> createRemoteBridge() {
            return (RemoteProviderBridge.ProviderGenericInput<T>) RemoteProviderBridge.Factory.<T>createWithGenericInput();
        }
    }
}
