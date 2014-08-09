package de.probst.ba.core;

import de.probst.ba.core.local.InMemoryDataDemos;
import de.probst.ba.core.local.InMemoryRemoteActorCloud;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // Create a new actor cloud
        RemoteActorCloud cloud = new InMemoryRemoteActorCloud();

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorA = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000);

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorB = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000);

        // Register an actor with 10 MB down and 1 MB up
        LocalActor actorC = cloud.registerLocalActor(10 * 1000 * 1000, 1 * 1000 * 1000);

        Data dataA = InMemoryDataDemos.MOVIE_A;

        // Add data to actor A
        actorA.getAllData().putIfAbsent(dataA.getHash(), dataA);
    }
}
