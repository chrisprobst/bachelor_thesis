package de.probst.ba.core.diagnostic;

import java.time.Duration;
import java.time.Instant;

/**
 * Created by chrisprobst on 22.08.14.
 */
public class AbstractTimeCVSDiagnostic extends AbstractCVSDiagnostic {

    private Instant timeStamp;

    protected void writeDuration(int elementWidth) {
        Duration duration = Duration.between(timeStamp, Instant.now());
        writeElement(duration.toMillis() / 1000.0, elementWidth);
    }

    protected Instant getTimeStamp() {
        return timeStamp;
    }

    protected void setTimeStamp() {
        timeStamp = Instant.now();
    }
}
