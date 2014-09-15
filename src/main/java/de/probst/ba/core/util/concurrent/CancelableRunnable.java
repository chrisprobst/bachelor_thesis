package de.probst.ba.core.util.concurrent;

/**
 * Created by chrisprobst on 15.09.14.
 */
public interface CancelableRunnable extends Runnable {

    void cancel();
}
