package de.probst.ba.core.statistic;

import de.probst.ba.core.net.peer.Peer;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class ChunkCompletionStatistic extends AbstractStatistic {

    private final Collection<Peer> peers;
    private final String dataInfoHash;

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
        peers.stream()
             .map(peer -> peer.getDataBase().get(dataInfoHash))
             .map(dataInfo -> dataInfo == null ? 0.0 : dataInfo.getPercentage())
             .forEach(csv::writeElement);
        csv.writeLine();
    }

    public ChunkCompletionStatistic(String name, Collection<Peer> peers, String dataInfoHash) {
        super(name);
        Objects.requireNonNull(peers);
        this.peers = peers;
        this.dataInfoHash = dataInfoHash;
    }
}
