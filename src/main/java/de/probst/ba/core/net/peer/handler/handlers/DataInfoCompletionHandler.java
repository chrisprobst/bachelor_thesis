package de.probst.ba.core.net.peer.handler.handlers;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandlerAdapter;

import java.util.concurrent.CountDownLatch;

/**
 * Created by chrisprobst on 08.09.14.
 */
public final class DataInfoCompletionHandler extends LeecherPeerHandlerAdapter {

    private final CountDownLatch countDownLatch;

    public DataInfoCompletionHandler(int dataInfoCount) {
        countDownLatch = new CountDownLatch(dataInfoCount);
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
        countDownLatch.countDown();
    }
}
