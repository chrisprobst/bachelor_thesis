package de.probst.ba.core;

import java.util.Collection;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface LocalActor extends Actor, Runnable  {

    Collection<Data> getAllData();
}
