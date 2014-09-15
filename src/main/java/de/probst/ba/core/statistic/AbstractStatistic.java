package de.probst.ba.core.statistic;

import de.probst.ba.core.util.io.CSV;

import java.util.Objects;

/**
 * Created by chrisprobst on 04.09.14.
 */
public abstract class AbstractStatistic {

    private final String name;

    protected final CSV csv = new CSV();

    protected abstract void doWriteStatistic();

    public AbstractStatistic(String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public synchronized void writeStatistic() {
        doWriteStatistic();
    }

    @Override
    public synchronized String toString() {
        return csv.toString();
    }
}
