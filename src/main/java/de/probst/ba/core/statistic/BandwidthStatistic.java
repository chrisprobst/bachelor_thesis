package de.probst.ba.core.statistic;

import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class BandwidthStatistic extends AbstractStatistic {

    private final Collection<Peer> peers;
    private final Function<BandwidthStatisticState, Number> bandwidthMapper;

    private void writeHeader() {
        csv.writeElement("Time");
        peers.forEach(peer -> csv.writeElement(peer.getPeerId().getSocketAddress()));
        csv.writeLine();
    }


    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration();
        peers.forEach(peer -> csv.writeElement(bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue()));
        csv.writeLine();
    }

    public BandwidthStatistic(String name,
                              Collection<Peer> peers,
                              Function<BandwidthStatisticState, Number> bandwidthMapper) {
        super(name);
        Objects.requireNonNull(peers);
        Objects.requireNonNull(bandwidthMapper);
        this.peers = peers;
        this.bandwidthMapper = bandwidthMapper;
    }
}
