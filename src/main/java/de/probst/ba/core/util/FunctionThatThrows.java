package de.probst.ba.core.util;

@FunctionalInterface
public interface FunctionThatThrows<T, R, E extends Throwable> {
    R apply(T t) throws E;
}