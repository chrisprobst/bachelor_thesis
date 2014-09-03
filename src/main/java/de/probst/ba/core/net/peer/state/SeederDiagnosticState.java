package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.net.peer.Peer;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class SeederDiagnosticState extends DiagnosticState {

    private final long maxUploadRate;
    private final long averageUploadRate;
    private final long currentUploadRate;
    private final long totalUploaded;

    public SeederDiagnosticState(Peer peer,
                                 long maxUploadRate,
                                 long averageUploadRate,
                                 long currentUploadRate,
                                 long totalUploaded) {
        super(peer);
        this.maxUploadRate = maxUploadRate;
        this.averageUploadRate = averageUploadRate;
        this.currentUploadRate = currentUploadRate;
        this.totalUploaded = totalUploaded;
    }

    /**
     * @return The maximal upload rate.
     */
    public long getMaxUploadRate() {
        return maxUploadRate;
    }

    /**
     * @return The average upload rate.
     */
    public long getAverageUploadRate() {
        return averageUploadRate;
    }

    /**
     * @return The current upload rate.
     */
    public long getCurrentUploadRate() {
        return currentUploadRate;
    }

    /**
     * @return The total upload rate.
     */
    public long getTotalUploaded() {
        return totalUploaded;
    }

    /**
     * @return The current upload ratio.
     */
    public double getCurrentUploadRatio() {
        return currentUploadRate / (double) maxUploadRate;
    }

    /**
     * @return The average upload ratio.
     */
    public double getAverageUploadRatio() {
        return averageUploadRate / (double) maxUploadRate;
    }
}
