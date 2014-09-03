package de.probst.ba.core.diagnostic;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class ChunkCompletionCSV extends CSV {

    private final boolean total;
    private final String dataInfoHash;

    private void writeHeader(Collection<Peer> peers) {
        writeElement("Time", Config.getDefaultCVSElementWidth());

        if (total) {
            writeElement(
                    "Total percentage",
                    Config.getDefaultCVSElementWidth());
        } else {
            for (Peer peer : peers) {
                writeElement(
                        peer.getPeerId().getAddress(),
                        Config.getDefaultCVSElementWidth());
            }
        }

        writeLine();
    }

    private void writeTotalStatus(Collection<Peer> peers) {
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
        writeElement(totalPercentage,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualStatus(Collection<Peer> peers) {
        for (Peer peer : peers) {
            DataInfo dataInfo = peer.getDataBase().get(dataInfoHash);
            if (dataInfo != null) {
                writeElement(dataInfo.getPercentage(),
                        Config.getDefaultCVSElementWidth());
            } else {
                writeElement(0.0,
                        Config.getDefaultCVSElementWidth());
            }
        }
    }

    public ChunkCompletionCSV(boolean total, String dataInfoHash) {
        Objects.requireNonNull(dataInfoHash);
        this.total = total;
        this.dataInfoHash = dataInfoHash;
    }

    public synchronized void writeStatus(Collection<Peer> peers) {
        Objects.requireNonNull(peers);

        if (isFirstElement()) {
            writeHeader(peers);
        }

        writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalStatus(peers);
        } else {
            writeIndividualStatus(peers);
        }

        writeLine();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }
}
