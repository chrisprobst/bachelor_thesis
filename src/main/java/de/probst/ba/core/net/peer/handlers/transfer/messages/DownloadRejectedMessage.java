package de.probst.ba.core.net.peer.handlers.transfer.messages;

import java.io.Serializable;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadRejectedMessage implements Serializable {

    private final Exception cause;

    public DownloadRejectedMessage(Exception cause) {
        this.cause = cause;
    }

    public Exception getCause() {
        return cause;
    }
}
