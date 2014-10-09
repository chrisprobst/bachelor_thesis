package de.probst.ba.core.media.database;

import java.io.IOException;

/**
 * Created by chrisprobst on 09.10.14.
 */
public final class DataInsertException extends IOException {

    public DataInsertException() {
    }

    public DataInsertException(String message) {
        super(message);
    }

    public DataInsertException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataInsertException(Throwable cause) {
        super(cause);
    }
}
