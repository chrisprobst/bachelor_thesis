package de.probst.ba.core.diagnostic;

import de.probst.ba.core.Config;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.state.SeederDiagnosticState;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class UploadBandwidthCSV extends CSV {

    private final boolean total;

    private void writeHeader(Collection<Peer> peers) {
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

    private void writeTotalUpload(Collection<Peer> peers) {
        double totalUpload = 0;
        int cnt = 0;
        for (Peer peer : peers) {
            if (peer instanceof Seeder) {
                Seeder seeder = (Seeder) peer;
                SeederDiagnosticState state = seeder.getDiagnosticState();
                totalUpload += state.getAverageUploadRatio();
                cnt++;
            }
        }
        writeElement(totalUpload / cnt,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualUpload(Collection<Peer> peers) {
        peers.stream()
                .filter(peer -> peer instanceof Seeder)
                .map(peer -> (Seeder) peer)
                .forEach(seeder -> {
                    SeederDiagnosticState state = seeder.getDiagnosticState();
                    writeElement(state.getAverageUploadRatio(), Config.getDefaultCVSElementWidth());
                });
    }

    public UploadBandwidthCSV(boolean total) {
        this.total = total;
    }

    public boolean isTotal() {
        return total;
    }

    public synchronized void writeStatus(Collection<Peer> peers) {
        Objects.requireNonNull(peers);

        if (isFirstElement()) {
            writeHeader(peers);
        }

        writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalUpload(peers);
        } else {
            writeIndividualUpload(peers);
        }

        writeLine();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }
}
