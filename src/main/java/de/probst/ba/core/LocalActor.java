package de.probst.ba.core;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface LocalActor extends Actor, Runnable  {

    ConcurrentMap<String, Data> getAllData();
}
