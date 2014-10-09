package de.probst.ba.core.media.database;

import java.io.IOException;

/**
 * Created by chrisprobst on 09.10.14.
 */
public final class DataLookupException extends IOException {

    public DataLookupException() {
    }

    public DataLookupException(String message) {
        super(message);
    }

    public DataLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataLookupException(Throwable cause) {
        super(cause);
    }
}
