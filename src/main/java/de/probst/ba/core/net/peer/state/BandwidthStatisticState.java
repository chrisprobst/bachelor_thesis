package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.net.peer.Peer;

/**
 * Created by chrisprobst on 03.09.14.
 */
public final class BandwidthStatisticState extends PeerState {

    private final long maxUploadRate;
    private final long averageUploadRate;
    private final long currentUploadRate;
    private final long totalUploaded;

    private final long maxDownloadRate;
    private final long averageDownloadRate;
    private final long currentDownloadRate;
    private final long totalDownloaded;

    public BandwidthStatisticState(Peer peer,
                                   long maxUploadRate,
                                   long averageUploadRate,
                                   long currentUploadRate,
                                   long totalUploaded,
                                   long maxDownloadRate,
                                   long averageDownloadRate,
                                   long currentDownloadRate,
                                   long totalDownloaded) {
        super(peer);
        this.maxUploadRate = maxUploadRate;
        this.averageUploadRate = averageUploadRate;
        this.currentUploadRate = currentUploadRate;
        this.totalUploaded = totalUploaded;
        this.maxDownloadRate = maxDownloadRate;
        this.averageDownloadRate = averageDownloadRate;
        this.currentDownloadRate = currentDownloadRate;
        this.totalDownloaded = totalDownloaded;
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


    /**
     * @return The maximal download rate.
     */
    public long getMaxDownloadRate() {
        return maxDownloadRate;
    }

    /**
     * @return The average download rate.
     */
    public long getAverageDownloadRate() {
        return averageDownloadRate;
    }

    /**
     * @return The current download rate.
     */
    public long getCurrentDownloadRate() {
        return currentDownloadRate;
    }

    /**
     * @return The total download rate.
     */
    public long getTotalDownloaded() {
        return totalDownloaded;
    }

    /**
     * @return The current download ratio.
     */
    public double getCurrentDownloadRatio() {
        return currentDownloadRate / (double) maxDownloadRate;
    }

    /**
     * @return The average download ratio.
     */
    public double getAverageDownloadRatio() {
        return averageDownloadRate / (double) maxDownloadRate;
    }
}
