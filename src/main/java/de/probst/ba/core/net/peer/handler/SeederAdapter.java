package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class SeederAdapter implements SeederHandler {
    
    @Override
    public void announced(Seeder seeder, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {

    }

    @Override
    public void uploadRejected(Seeder seeder, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void uploadStarted(Seeder seeder, TransferManager transferManager) {

    }

    @Override
    public void uploadSucceeded(Seeder seeder, TransferManager transferManager) {

    }
}
