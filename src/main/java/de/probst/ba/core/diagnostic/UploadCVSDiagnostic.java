package de.probst.ba.core.diagnostic;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.SeederState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class UploadCVSDiagnostic extends AbstractTimeCVSDiagnostic {

    private boolean total;
    private Collection<Peer> peers;

    private void writeHeader() {
        writeElement("Time", Config.getDefaultCVSElementWidth());

        if (total) {
            writeElement(
                    "Total upload",
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

    private void writeTotalUpload() {
        double totalUpload = 0;
        int cnt = 0;
        for (Peer peer : peers) {
            if (peer instanceof Seeder) {
                SeederState peerState = ((Seeder) peer).getPeerState();
                if (!peerState.getUploads().isEmpty()) {
                    totalUpload++;
                }
                cnt++;
            }
        }
        totalUpload /= cnt;

        writeElement(totalUpload,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualUpload() {
        peers.stream().filter(peer -> peer instanceof Seeder).forEach(peer -> {
            SeederState peerState = ((Seeder) peer).getPeerState();
            if (peerState.getUploads().isEmpty()) {
                writeElement(0.0,
                        Config.getDefaultCVSElementWidth());
            } else {
                writeElement(1.0,
                        Config.getDefaultCVSElementWidth());
            }
        });
    }

    public synchronized void writeStatus() {
        if (peers == null) {
            throw new IllegalStateException("peers == null");
        }

        if (getTimeStamp() == null) {
            setTimeStamp();
            writeHeader();
        }

        writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalUpload();
        } else {
            writeIndividualUpload();
        }

        writeLine();
    }

    @Override
    public synchronized String getCVSString() {
        return super.getCVSString();
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

    @Override
    public void uploadStarted(Seeder seeder, TransferManager transferManager) {
        writeStatus();
    }

    @Override
    public void uploadSucceeded(Seeder seeder, TransferManager transferManager) {
        writeStatus();
    }
}