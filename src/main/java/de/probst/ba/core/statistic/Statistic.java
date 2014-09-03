package de.probst.ba.core.statistic;

import de.probst.ba.core.util.io.CSV;

/**
 * Created by chrisprobst on 04.09.14.
 */
public abstract class Statistic {

    protected final CSV csv = new CSV();

    protected abstract void doWriteStatistic();

    public synchronized void writeStatistic() {
        doWriteStatistic();
    }

    @Override
    public synchronized String toString() {
        return csv.toString();
    }
}
