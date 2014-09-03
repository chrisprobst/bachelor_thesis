package de.probst.ba.core.util.collections;

/**
 * Created by chrisprobst on 17.08.14.
 */
public class Tuple2<A, B> extends Tuple1<A> {

    private final B second;

    public Tuple2(A first, B second) {
        super(first);
        this.second = second;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B second() {
        return second;
    }

    @Override
    public String toString() {
        return "(" + first() + ", " + second + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tuple2 tuple2 = (Tuple2) o;

        if (second != null ? !second.equals(tuple2.second) : tuple2.second != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
