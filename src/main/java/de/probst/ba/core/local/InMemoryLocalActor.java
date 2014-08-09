package de.probst.ba.core.local;

import de.probst.ba.core.AbstractLocalActor;
import de.probst.ba.core.RemoteActor;
import de.probst.ba.core.RemoteActorCloud;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryLocalActor extends AbstractLocalActor {

    protected InMemoryLocalActor(RemoteActorCloud remoteActorCloud, long id, long downloadRate, long uploadRate) {
        super(remoteActorCloud, id, downloadRate, uploadRate);
    }

    private final RemoteActor remoteActor = new InMemoryRemoteActor(
            getRemoteActorCloud(),
            getId(),
            getDownloadRate(),
            getUploadRate(),
            this
    );

    public RemoteActor getRemoteActor() {
        return remoteActor;
    }

    @Override
    public void run() {
        System.out.println("Running actor with id: " + getId());

        try {

            getRemoteActorCloud().getAllDataAsync().thenAccept(map -> {
                System.out.println(map);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
