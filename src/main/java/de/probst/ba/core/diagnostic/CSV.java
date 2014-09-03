package de.probst.ba.core.diagnostic;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class CSV {

    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean firstElement = true;
    private Instant timeStamp;

    private void write(String element) {
        if (firstElement) {
            firstElement = false;
        }
        stringBuilder.append(element);
    }

    public void writeDuration(int elementWidth) {
        if (timeStamp == null) {
            resetTimeStamp();
        }
        Duration duration = Duration.between(timeStamp, Instant.now());
        writeElement(duration.toMillis() / 1000.0, elementWidth);
    }

    public boolean isFirstElement() {
        return firstElement;
    }

    public void resetTimeStamp() {
        timeStamp = Instant.now();
    }

    public void writeLine() {
        write(System.lineSeparator());
    }

    public void writeElement(Object element, int elementWidth) {
        Objects.requireNonNull(element);
        String s = element.toString();

        if (s.length() > elementWidth) {
            throw new IllegalArgumentException("s.length() > elementWidth");
        }

        write(s);
        for (int i = 0; i < elementWidth - s.length(); i++) {
            write(" ");
        }
    }

    public void writeElements(Stream<?> elements, int elementWidth) throws IOException {
        for (Object element : (Iterable<?>) elements) {
            writeElement(element, elementWidth);
        }
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
