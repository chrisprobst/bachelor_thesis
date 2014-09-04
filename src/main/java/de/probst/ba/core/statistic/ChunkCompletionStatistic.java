package de.probst.ba.core.statistic;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Queue;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class ChunkCompletionStatistic extends AbstractFileStatistic {

    private final Queue<Peer> peers;
    private final String dataInfoHash;
    private final boolean total;

    public ChunkCompletionStatistic(Path csvPath, Queue<Peer> peers, String dataInfoHash, boolean total) {
        super(csvPath);
        Objects.requireNonNull(peers);
        this.peers = peers;
        this.dataInfoHash = dataInfoHash;
        this.total = total;
    }

    private void writeHeader() {
        csv.writeElement("Time", Config.getDefaultCVSElementWidth());

        if (total) {
            csv.writeElement("Total percentage", Config.getDefaultCVSElementWidth());
        } else {
            for (Peer peer : peers) {
                csv.writeElement(peer.getPeerId().getAddress(), Config.getDefaultCVSElementWidth());
            }
        }

        csv.writeLine();
    }

    private void writeTotalStatus() {
        double totalPercentage = 0;
        int cnt = 0;
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo != null) {
                totalPercentage += dataInfo.getPercentage();
            }
            cnt++;
        }
        totalPercentage /= cnt;
        csv.writeElement(totalPercentage, Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualStatus() {
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo != null) {
                csv.writeElement(dataInfo.getPercentage(), Config.getDefaultCVSElementWidth());
            } else {
                csv.writeElement(0.0, Config.getDefaultCVSElementWidth());
            }
        }
    }

    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalStatus();
        } else {
            writeIndividualStatus();
        }

        csv.writeLine();
    }
}
