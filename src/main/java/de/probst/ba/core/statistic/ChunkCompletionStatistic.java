package de.probst.ba.core.statistic;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class ChunkCompletionStatistic extends AbstractStatistic {

    private final Collection<Peer> peers;
    private final String dataInfoHash;
    private final boolean total;

    private void writeHeader() {
        csv.writeElement("Time");

        if (total) {
            csv.writeElement("TotalAverage");
            csv.writeElement("TotalLowerDeviation");
            csv.writeElement("TotalUpperDeviation");
        } else {
            for (Peer peer : peers) {
                csv.writeElement(peer.getPeerId().getSocketAddress());
            }
        }

        csv.writeLine();
    }

    private void writeTotalStatus() {
        int cnt = 0;
        double totalAverage = 0;
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo != null) {
                totalAverage += dataInfo.getPercentage();
                cnt++;
            }
        }
        totalAverage /= cnt;

        double totalLowerDeviation = 0;
        double totalUpperDeviation = 0;
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo == null) {
                continue;
            }
            double peerChunkCompletion = dataInfo.getPercentage();

            if (peerChunkCompletion >= totalAverage) {
                totalUpperDeviation += peerChunkCompletion - totalAverage;
            } else {
                totalLowerDeviation += totalAverage - peerChunkCompletion;
            }
        }

        totalUpperDeviation /= cnt;
        totalLowerDeviation /= cnt;

        if (Double.isFinite(totalAverage)) {
            csv.writeElement(totalAverage);
        }
        if (Double.isFinite(totalLowerDeviation)) {
            csv.writeElement(totalLowerDeviation);
        }
        if (Double.isFinite(totalUpperDeviation)) {
            csv.writeElement(totalUpperDeviation);
        }
    }

    private void writeIndividualStatus() {
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo != null) {
                csv.writeElement(dataInfo.getPercentage());
            } else {
                csv.writeElement(0.0);
            }
        }
    }

    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration();

        if (total) {
            writeTotalStatus();
        } else {
            writeIndividualStatus();
        }

        csv.writeLine();
    }

    public ChunkCompletionStatistic(String name, Collection<Peer> peers, String dataInfoHash, boolean total) {
        super(name);
        Objects.requireNonNull(peers);
        this.peers = peers;
        this.dataInfoHash = dataInfoHash;
        this.total = total;
    }

    public boolean isTotal() {
        return total;
    }
}
