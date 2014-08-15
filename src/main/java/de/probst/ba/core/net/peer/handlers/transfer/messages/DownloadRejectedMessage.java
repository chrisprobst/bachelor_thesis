package de.probst.ba.core.net.peer.handlers.transfer.messages;

import java.io.Serializable;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadRejectedMessage implements Serializable {

    private final Throwable cause;

    public DownloadRejectedMessage(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
