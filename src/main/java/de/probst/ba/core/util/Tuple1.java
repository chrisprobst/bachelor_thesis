package de.probst.ba.core.util;

/**
 * Created by chrisprobst on 17.08.14.
 */
public class Tuple1<A> extends Tuple {

    private final A first;

    public Tuple1(A first) {
        this.first = first;
    }

    @Override
    @SuppressWarnings("unchecked")
    public A first() {
        return first;
    }

    @Override
    public String toString() {
        return "(" + first + ", )";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple1 tuple1 = (Tuple1) o;

        if (first != null ? !first.equals(tuple1.first) : tuple1.first != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return first != null ? first.hashCode() : 0;
    }
}
