package de.probst.ba.core.net.peer.netty.handlers.transfer.messages;

import java.io.Serializable;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class UploadRejectedMessage implements Serializable {

    private final Throwable cause;

    public UploadRejectedMessage(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
