package me.alek.honey.util;

public interface Provider<T, P extends Provider<T, P, A>, A extends ProviderAdapter<T, P, A>> {

    A adapter();

    interface ProviderNoInput<T> extends Provider<T, ProviderNoInput<T>, ProviderAdapter.ProviderNoInput<T>> {

        T get();

        default ProviderAdapter.ProviderNoInput<T> adapter() {
            return new ProviderAdapter.ProviderNoInput<T>() {
            };
        }
    }

    interface ProviderOneInput<T, I1> extends Provider<T, ProviderOneInput<T, I1>, ProviderAdapter.ProviderOneInput<T, I1>> {

        T get(I1 i1);

        default ProviderAdapter.ProviderOneInput<T, I1> adapter() {
            return new ProviderAdapter.ProviderOneInput<T, I1>() {
            };
        }
    }

    interface ProviderTwoInput<T, I1, I2> extends Provider<T, ProviderTwoInput<T, I1, I2>, ProviderAdapter.ProviderTwoInput<T, I1, I2>> {

        T get(I1 i1, I2 i2);

        default ProviderAdapter.ProviderTwoInput<T, I1, I2> adapter() {
            return new ProviderAdapter.ProviderTwoInput<T, I1, I2>() {
            };
        }
    }

    interface ProviderGenericInput<T> extends Provider<T, ProviderGenericInput<T>, ProviderAdapter.ProviderGenericInput<T>> {

        T get(Object[] array);

        default ProviderAdapter.ProviderGenericInput<T> adapter() {
            return new ProviderAdapter.ProviderGenericInput<T>() {
            };
        }
    }
}
