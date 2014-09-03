package de.probst.ba.core.statistic;

import de.probst.ba.core.Config;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.state.SeederStatisticState;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class UploadBandwidthStatistic extends ScheduledStatistic {

    private final Queue<Peer> peers;
    private final boolean total;

    private void writeHeader() {
        csv.writeElement("Time", Config.getDefaultCVSElementWidth());

        if (total) {
            csv.writeElement(
                    "Total upload",
                    Config.getDefaultCVSElementWidth());
        } else {
            for (Peer peer : peers) {
                csv.writeElement(
                        peer.getPeerId().getAddress(),
                        Config.getDefaultCVSElementWidth());
            }
        }

        csv.writeLine();
    }

    private void writeTotalUpload() {
        double totalUpload = 0;
        int cnt = 0;
        for (Peer peer : peers) {
            if (peer instanceof Seeder) {
                Seeder seeder = (Seeder) peer;
                SeederStatisticState state = seeder.getStatisticState();
                totalUpload += state.getAverageUploadRatio();
                cnt++;
            }
        }
        csv.writeElement(totalUpload / cnt,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualUpload() {
        peers.stream()
                .filter(peer -> peer instanceof Seeder)
                .map(peer -> (Seeder) peer)
                .forEach(seeder -> {
                    SeederStatisticState state = seeder.getStatisticState();
                    csv.writeElement(state.getAverageUploadRatio(), Config.getDefaultCVSElementWidth());
                });
    }

    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration(Config.getDefaultCVSElementWidth());

        if (total) {
            writeTotalUpload();
        } else {
            writeIndividualUpload();
        }

        csv.writeLine();
    }

    public UploadBandwidthStatistic(Path csvPath,
                                    ScheduledExecutorService scheduledExecutorService,
                                    long delay,
                                    Queue<Peer> peers,
                                    boolean total) {
        super(csvPath, scheduledExecutorService, delay);
        Objects.requireNonNull(peers);
        this.peers = peers;
        this.total = total;
    }
}
