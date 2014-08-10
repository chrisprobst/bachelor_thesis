package de.probst.ba.core.legacy;

import de.probst.ba.core.legacy.local.InMemoryDataDemos;
import de.probst.ba.core.legacy.local.InMemoryRemoteActorCloud;
import de.probst.ba.core.logic.DataInfo;

import java.io.IOException;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // Create a new actor cloud
        RemoteActorCloud cloud = new InMemoryRemoteActorCloud();

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorA = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000, FulfillPolicy.Server);

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorB = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000, FulfillPolicy.Client);

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorC = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000, FulfillPolicy.Client);

        DataInfo dataInfoA = InMemoryDataDemos.MOVIE_A;
        DataInfo dataInfoB = InMemoryDataDemos.MOVIE_B;

        // Add data to actor
        actorA.getAllData().putIfAbsent(dataInfoA.getHash(), dataInfoA);
        actorA.getAllData().putIfAbsent(dataInfoB.getHash(), dataInfoB);
    }
}
