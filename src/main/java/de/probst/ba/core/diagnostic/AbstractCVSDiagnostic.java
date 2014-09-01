package de.probst.ba.core.diagnostic;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by chrisprobst on 22.08.14.
 */
public abstract class AbstractCVSDiagnostic extends DiagnosticAdapter {

    private final StringBuilder stringBuilder = new StringBuilder();

    protected void writeLine() {
        stringBuilder.append(System.lineSeparator());
    }

    protected void writeElement(Object element, int elementWidth) {
        Objects.requireNonNull(element);
        String s = element.toString();

        if (s.length() > elementWidth) {
            throw new IllegalArgumentException("s.length() > elementWidth");
        }

        stringBuilder.append(s);
        for (int i = 0; i < elementWidth - s.length(); i++) {
            stringBuilder.append(" ");
        }
    }

    protected void writeElements(Stream<?> elements, int elementWidth) throws IOException {
        for (Object element : (Iterable<?>) elements) {
            writeElement(element, elementWidth);
        }
    }

    public String getCVSString() {
        return stringBuilder.toString();
    }
}
