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

    private final long averageMetaUploadRate;
    private final long currentMetaUploadRate;
    private final long totalMetaUploaded;

    private final long maxDownloadRate;
    private final long averageDownloadRate;
    private final long currentDownloadRate;
    private final long totalDownloaded;

    private final long averageMetaDownloadRate;
    private final long currentMetaDownloadRate;
    private final long totalMetaDownloaded;

    public BandwidthStatisticState(Peer peer,
                                   long maxUploadRate,
                                   long averageUploadRate,
                                   long currentUploadRate,
                                   long totalUploaded,
                                   //
                                   long averageMetaUploadRate,
                                   long currentMetaUploadRate,
                                   long totalMetaUploaded,
                                   //
                                   long maxDownloadRate,
                                   long averageDownloadRate,
                                   long currentDownloadRate,
                                   long totalDownloaded,
                                   //
                                   long averageMetaDownloadRate,
                                   long currentMetaDownloadRate,
                                   long totalMetaDownloaded) {
        super(peer);
        this.maxUploadRate = maxUploadRate;
        this.averageUploadRate = averageUploadRate;
        this.currentUploadRate = currentUploadRate;
        this.totalUploaded = totalUploaded;
        //
        this.averageMetaUploadRate = averageMetaUploadRate;
        this.currentMetaUploadRate = currentMetaUploadRate;
        this.totalMetaUploaded = totalMetaUploaded;
        //
        this.maxDownloadRate = maxDownloadRate;
        this.averageDownloadRate = averageDownloadRate;
        this.currentDownloadRate = currentDownloadRate;
        this.totalDownloaded = totalDownloaded;
        //
        this.averageMetaDownloadRate = averageMetaDownloadRate;
        this.currentMetaDownloadRate = currentMetaDownloadRate;
        this.totalMetaDownloaded = totalMetaDownloaded;
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

    //

    public long getAverageMetaUploadRate() {
        return averageMetaUploadRate;
    }

    public long getCurrentMetaUploadRate() {
        return currentMetaUploadRate;
    }

    public long getTotalMetaUploaded() {
        return totalMetaUploaded;
    }

    //

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

    //

    public long getAverageMetaDownloadRate() {
        return averageMetaDownloadRate;
    }

    public long getCurrentMetaDownloadRate() {
        return currentMetaDownloadRate;
    }

    public long getTotalMetaDownloaded() {
        return totalMetaDownloaded;
    }
}
