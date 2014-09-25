package de.probst.ba.core.util.io;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by chrisprobst on 03.09.14.
 */
public final class CSV {

    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean firstElement = true;
    private long timeStamp = -1;

    private void write(String element) {
        if (firstElement) {
            firstElement = false;
        }
        stringBuilder.append(element);
    }

    public void writeDuration() {
        if (timeStamp < 0) {
            resetTimeStamp();
        }
        writeElement((System.currentTimeMillis() - timeStamp) / 1000.0);
    }

    public boolean isFirstElement() {
        return firstElement;
    }

    public void resetTimeStamp() {
        timeStamp = System.currentTimeMillis();
    }

    public void writeLine() {
        write(System.lineSeparator());
    }

    public void writeElement(Object element) {
        Objects.requireNonNull(element);
        String s = element.toString();
        write(s);
        write(" ");
    }

    public void writeElements(Stream<?> elements) {
        for (Object element : (Iterable<?>) elements) {
            writeElement(element);
        }
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
