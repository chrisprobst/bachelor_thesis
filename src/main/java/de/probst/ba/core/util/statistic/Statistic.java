package de.probst.ba.core.util.statistic;

import de.probst.ba.core.util.io.CSV;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class Statistic<T> {

    private final CSV csv = new CSV();
    private final String name;
    private final Collection<T> elements;
    private final Function<T, String> headerMapper;
    private final Function<T, Number> valueMapper;

    public Statistic(String name,
                     Collection<T> elements,
                     Function<T, String> headerMapper,
                     Function<T, Number> valueMapper) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(elements);
        Objects.requireNonNull(headerMapper);
        Objects.requireNonNull(valueMapper);
        this.name = name;
        this.elements = elements;
        this.headerMapper = headerMapper;
        this.valueMapper = valueMapper;
    }

    public String getName() {
        return name;
    }

    public synchronized void writeNextEntry() {
        try {
            if (csv.isFirstElement()) {
                csv.writeElement("time");
                elements.forEach(element -> csv.writeElement(headerMapper.apply(element)));
                csv.writeLine();
            }

            csv.writeDuration();
            elements.forEach(element -> csv.writeElement(valueMapper.apply(element).doubleValue()));
            csv.writeLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String toString() {
        return csv.toString();
    }
}
