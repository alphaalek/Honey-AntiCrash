package me.alek.honey.util;

public interface Provider<T, P extends Provider<T, P, A>, A extends ProviderBridge<T, P, A>> {

    A createBridge();

    interface ProviderNoInput<T> extends Provider<T, ProviderNoInput<T>, ProviderBridge.ProviderNoInput<T>> {

        T get();

        default ProviderBridge.ProviderNoInput<T> createBridge() {
            return (ProviderBridge.ProviderNoInput<T>) ProviderBridge.<T>getWithNoInput();
        }
    }

    interface ProviderOneInput<T, I1> extends Provider<T, ProviderOneInput<T, I1>, ProviderBridge.ProviderOneInput<T, I1>> {

        T get(I1 i1);

        default ProviderBridge.ProviderOneInput<T, I1> createBridge() {
            return (ProviderBridge.ProviderOneInput<T, I1>) ProviderBridge.<T, I1>getWithOneInput();
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends Provider<T, ProviderTwoInput<T, I1, I2>, ProviderBridge.ProviderTwoInput<T, I1, I2>> {

        T get(I1 i1, I2 i2);

        default ProviderBridge.ProviderTwoInput<T, I1, I2> createBridge() {
            return (ProviderBridge.ProviderTwoInput<T, I1, I2>) ProviderBridge.<T, I1, I2>getWithTwoInput();
        }
    }

    interface ProviderGenericInput<T> extends Provider<T, ProviderGenericInput<T>, ProviderBridge.ProviderGenericInput<T>> {

        T get(Object[] array);

        default ProviderBridge.ProviderGenericInput<T> createBridge() {
            return (ProviderBridge.ProviderGenericInput<T>) ProviderBridge.<T>getWithGenericInput();
        }
    }
}
