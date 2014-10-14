package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chrisprobst on 14.10.14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EstimatedMessageSize {
    long value();
}
