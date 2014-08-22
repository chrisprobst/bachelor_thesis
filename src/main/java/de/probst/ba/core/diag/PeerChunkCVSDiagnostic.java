package de.probst.ba.core.diag;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class PeerChunkCVSDiagnostic extends AbstractTimeCVSDiagnostic {

    private boolean total;
    private Collection<Peer> peers;
    private String dataInfoHash;

    private void writeHeader() {
        writeElement("Time", Config.getDefaultCVSElementWidth());

        if (total) {
            writeElement(
                    "Total percentage",
                    Config.getDefaultCVSElementWidth());
        } else {
            for (Peer peer : peers) {
                writeElement(
                        peer.getNetworkState().getLocalPeerId().getAddress(),
                        Config.getDefaultCVSElementWidth());
            }
        }

        writeLine();
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
        writeElement(totalPercentage,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualStatus() {
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

    public synchronized void writeStatus() {
        if (peers == null) {
            throw new IllegalStateException("peers == null");
        }
        if (dataInfoHash == null) {
            throw new IllegalStateException("dataInfoHash == null");
        }

        if (getTimeStamp() == null) {
            setTimeStamp();
            writeHeader();
        }

        writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalStatus();
        } else {
            writeIndividualStatus();
        }

        writeLine();
    }

    public synchronized boolean isTotal() {
        return total;
    }

    public synchronized void setTotal(boolean total) {
        this.total = total;
    }

    public synchronized Collection<Peer> getPeers() {
        return peers;
    }

    public synchronized void setPeers(Collection<Peer> peers) {
        Objects.requireNonNull(peers);
        this.peers = new ArrayList<>(peers);
    }

    public synchronized String getDataInfoHash() {
        return dataInfoHash;
    }

    public synchronized void setDataInfoHash(String dataInfoHash) {
        Objects.requireNonNull(dataInfoHash);
        this.dataInfoHash = dataInfoHash;
    }

    @Override
    public synchronized String getCVSString() {
        return super.getCVSString();
    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {
        writeStatus();
    }
}
