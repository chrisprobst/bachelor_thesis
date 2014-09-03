package de.probst.ba.core.diagnostic;

import de.probst.ba.core.Config;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.state.SeederDiagnosticState;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 22.08.14.
 */
public final class UploadBandwidthCSV extends CSV implements Runnable, Closeable {

    private final ScheduledExecutorService scheduledExecutorService;
    private final Queue<Peer> peers;
    private final Path csvPath;
    private final long delay;
    private final boolean total;
    private Future<?> schedulation;

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
                Seeder seeder = (Seeder) peer;
                SeederDiagnosticState state = seeder.getDiagnosticState();
                totalUpload += state.getAverageUploadRatio();
                cnt++;
            }
        }
        writeElement(totalUpload / cnt,
                Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualUpload() {
        peers.stream()
                .filter(peer -> peer instanceof Seeder)
                .map(peer -> (Seeder) peer)
                .forEach(seeder -> {
                    SeederDiagnosticState state = seeder.getDiagnosticState();
                    writeElement(state.getAverageUploadRatio(), Config.getDefaultCVSElementWidth());
                });
    }

    private void writeStatus() {
        if (isFirstElement()) {
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

    public UploadBandwidthCSV(ScheduledExecutorService scheduledExecutorService, Queue<Peer> peers, Path csvPath, long delay, boolean total) {
        Objects.requireNonNull(scheduledExecutorService);
        Objects.requireNonNull(peers);
        Objects.requireNonNull(csvPath);
        this.scheduledExecutorService = scheduledExecutorService;
        this.peers = peers;
        this.csvPath = csvPath;
        this.delay = delay;
        this.total = total;
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    public synchronized void schedule() {
        if (schedulation != null) {
            throw new IllegalStateException("schedulation != null");
        }
        schedulation = scheduledExecutorService.schedule(
                this, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void run() {
        if (schedulation == null) {
            return;
        }
        schedulation = null;
        writeStatus();
        schedule();
    }

    public synchronized void cancel() {
        if (schedulation != null) {
            schedulation.cancel(false);
            schedulation = null;
        }
    }

    public void save() throws IOException {
        Files.write(csvPath, toString().getBytes());
    }

    @Override
    public synchronized void close() throws IOException {
        cancel();
        save();
    }
}
