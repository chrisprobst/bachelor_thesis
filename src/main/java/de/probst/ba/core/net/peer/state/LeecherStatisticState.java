package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.net.peer.Peer;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class LeecherStatisticState extends StatisticState {

    private final long maxDownloadRate;

    public LeecherStatisticState(Peer peer, long maxDownloadRate) {
        super(peer);
        this.maxDownloadRate = maxDownloadRate;
    }

    /**
     * @return The maximal download rate.
     */
    public long getMaxDownloadRate() {
        return maxDownloadRate;
    }
}
