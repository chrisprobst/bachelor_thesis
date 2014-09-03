package de.probst.ba.core.util.collections;

import java.util.NoSuchElementException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class Tuple {

    public static <A> Tuple1<A> of(A first) {
        return new Tuple1<>(first);
    }

    public static <A, B> Tuple2<A, B> of(A first, B second) {
        return new Tuple2<>(first, second);
    }

    public static <A, B, C> Tuple3<A, B, C> of(A first, B second, C third) {
        return new Tuple3<>(first, second, third);
    }

    public <A> A first() {
        throw new NoSuchElementException();
    }

    public <B> B second() {
        throw new NoSuchElementException();
    }

    public <C> C third() {
        throw new NoSuchElementException();
    }
}
