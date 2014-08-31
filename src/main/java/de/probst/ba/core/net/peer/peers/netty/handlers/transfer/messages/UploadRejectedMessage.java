package de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class UploadRejectedMessage implements Serializable {

    private final Throwable cause;

    public UploadRejectedMessage(Throwable cause) {
        Objects.requireNonNull(cause);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
