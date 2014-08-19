package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class RecordDiagnostic extends LoggingDiagnostic {

    public abstract static class Record implements Comparable<Record>, Serializable {

        private final Instant timeStamp = Instant.now();
        private final PeerId localPeerId;

        public Record(PeerId localPeerId) {
            Objects.requireNonNull(localPeerId);
            this.localPeerId = localPeerId;
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public PeerId getLocalPeerId() {
            return localPeerId;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "timeStamp=" + timeStamp +
                    ", localPeerId=" + localPeerId +
                    '}';
        }

        @Override
        public int compareTo(Record o) {
            return getTimeStamp().compareTo(o.getTimeStamp());
        }
    }

    public abstract static class DataInfoRecord extends Record {

        private final PeerId remotePeerId;
        private final Map<String, DataInfo> dataInfo;

        public DataInfoRecord(PeerId localPeerId,
                              PeerId remotePeerId,
                              Map<String, DataInfo> dataInfo) {
            super(localPeerId);
            Objects.requireNonNull(remotePeerId);

            this.remotePeerId = remotePeerId;
            this.dataInfo = dataInfo;
        }

        public PeerId getRemotePeerId() {
            return remotePeerId;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "remotePeerId=" + remotePeerId +
                    ", dataInfo=" + dataInfo +
                    "} " + super.toString();
        }
    }

    public final static class CollectedRecord extends DataInfoRecord {

        public CollectedRecord(PeerId localPeerId,
                               PeerId remotePeerId,
                               Map<String, DataInfo> dataInfo) {
            super(localPeerId, remotePeerId, dataInfo);
        }
    }

    public final static class AnnouncedRecord extends DataInfoRecord {

        public AnnouncedRecord(PeerId localPeerId,
                               PeerId remotePeerId,
                               Map<String, DataInfo> dataInfo) {
            super(localPeerId, remotePeerId, dataInfo);
        }
    }

    public abstract static class TransferRecord extends Record {

        private final Transfer transfer;

        public TransferRecord(PeerId localPeerId,
                              Transfer transfer) {
            super(localPeerId);
            this.transfer = transfer;
        }

        public Transfer getTransfer() {
            return transfer;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "transfer=" + transfer +
                    "} " + super.toString();
        }
    }

    public final static class DownloadRequestedRecord extends TransferRecord {
        public DownloadRequestedRecord(PeerId localPeerId,
                                       Transfer transfer) {
            super(localPeerId, transfer);
        }
    }

    public final static class DownloadRejectedRecord extends TransferRecord {
        private final Throwable cause;

        public DownloadRejectedRecord(PeerId localPeerId,
                                      Transfer transfer,
                                      Throwable cause) {
            super(localPeerId, transfer);
            Objects.requireNonNull(cause);
            this.cause = cause;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return super.toString() + " with cause: " + cause;
        }
    }

    public final static class DownloadFailedRecord extends TransferRecord {
        private final Throwable cause;

        public DownloadFailedRecord(PeerId localPeerId,
                                    Transfer transfer,
                                    Throwable cause) {
            super(localPeerId, transfer);
            Objects.requireNonNull(cause);
            this.cause = cause;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return super.toString() + " with cause: " + cause;
        }
    }

    public final static class DownloadStartedRecord extends TransferRecord {
        public DownloadStartedRecord(PeerId localPeerId,
                                     Transfer transfer) {
            super(localPeerId, transfer);
        }
    }

    public final static class DownloadProgressedRecord extends TransferRecord {
        public DownloadProgressedRecord(PeerId localPeerId,
                                        Transfer transfer) {
            super(localPeerId, transfer);
        }
    }

    public final static class DownloadSucceededRecord extends TransferRecord {
        public DownloadSucceededRecord(PeerId localPeerId,
                                       Transfer transfer) {
            super(localPeerId, transfer);
        }
    }


    public final static class UploadRejectedRecord extends TransferRecord {
        private final Throwable cause;

        public UploadRejectedRecord(PeerId localPeerId,
                                    Transfer transfer,
                                    Throwable cause) {
            super(localPeerId, transfer);
            Objects.requireNonNull(cause);
            this.cause = cause;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return super.toString() + " with cause: " + cause;
        }
    }

    public final static class UploadStartedRecord extends TransferRecord {
        public UploadStartedRecord(PeerId localPeerId,
                                   Transfer transfer) {
            super(localPeerId, transfer);
        }
    }

    public final static class UploadSucceededRecord extends TransferRecord {
        public UploadSucceededRecord(PeerId localPeerId,
                                     Transfer transfer) {
            super(localPeerId, transfer);
        }
    }

    private final Queue<Record> records = new ConcurrentLinkedQueue<>();

    public List<Record> getRecords() {
        return records.stream().collect(Collectors.toList());
    }

    ////
    //// DIAGNOSTICS METHODS
    ////

    @Override
    public void peerAnnouncedDataInfo(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        super.peerAnnouncedDataInfo(peer, remotePeerId, dataInfo);
        records.add(new AnnouncedRecord(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void peerCollectedDataInfo(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        super.peerCollectedDataInfo(peer, remotePeerId, dataInfo);
        records.add(new CollectedRecord(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void peerRejectedUpload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerRejectedUpload(peer, transferManager, cause);
        records.add(new UploadRejectedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void peerStartedUpload(Peer peer, TransferManager transferManager) {
        super.peerStartedUpload(peer, transferManager);
        records.add(new UploadStartedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerSucceededUpload(Peer peer, TransferManager transferManager) {
        super.peerSucceededUpload(peer, transferManager);
        records.add(new UploadSucceededRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerRequestedDownload(Peer peer, TransferManager transferManager) {
        super.peerRequestedDownload(peer, transferManager);
        records.add(new DownloadRequestedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerRejectedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerRejectedDownload(peer, transferManager, cause);
        records.add(new DownloadRejectedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void peerStartedDownload(Peer peer, TransferManager transferManager) {
        super.peerStartedDownload(peer, transferManager);
        records.add(new DownloadStartedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerProgressedDownload(Peer peer, TransferManager transferManager) {
        super.peerProgressedDownload(peer, transferManager);
        records.add(new DownloadProgressedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerSucceededDownload(Peer peer, TransferManager transferManager) {
        super.peerSucceededDownload(peer, transferManager);
        records.add(new DownloadSucceededRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerFailedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerFailedDownload(peer, transferManager, cause);
        records.add(new DownloadFailedRecord(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }
}
