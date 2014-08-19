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

    public enum RecordType {
        Announced, Collected,
        UploadStarted, UploadRejected, UploadSucceeded,
        DownloadRequested, DownloadRejected, DownloadStarted, DownloadProgressed, DownloadSucceeded, DownloadFailed
    }

    public final static class Record implements Comparable<Record>, Serializable {

        // DATA INFO

        public static Record announced(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Announced, peerId, remotePeerId, dataInfo, null, null);
        }

        public static Record collected(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Collected, peerId, remotePeerId, dataInfo, null, null);
        }

        // UPLOAD

        public static Record uploadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadStarted, peerId, null, null, transfer, null);
        }

        public static Record uploadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.UploadRejected, peerId, null, null, transfer, cause);
        }

        public static Record uploadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadSucceeded, peerId, null, null, transfer, null);
        }

        // DOWNLOAD

        public static Record downloadRequested(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadRequested, peerId, null, null, transfer, null);
        }

        public static Record downloadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.DownloadRejected, peerId, null, null, transfer, cause);
        }

        public static Record downloadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadStarted, peerId, null, null, transfer, null);
        }

        public static Record downloadProgressed(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadProgressed, peerId, null, null, transfer, null);
        }

        public static Record downloadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadSucceeded, peerId, null, null, transfer, null);
        }

        public static Record downloadFailed(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.DownloadFailed, peerId, null, null, transfer, cause);
        }

        private final Instant timeStamp = Instant.now();
        private final RecordType recordType;
        private final PeerId localPeerId;
        private final PeerId remotePeerId;
        private final Map<String, DataInfo> dataInfo;
        private final Transfer transfer;
        private final Throwable cause;

        private Record(RecordType recordType,
                       PeerId localPeerId,
                       PeerId remotePeerId,
                       Map<String, DataInfo> dataInfo,
                       Transfer transfer,
                       Throwable cause) {
            Objects.requireNonNull(localPeerId);
            Objects.requireNonNull(recordType);
            this.localPeerId = localPeerId;
            this.remotePeerId = remotePeerId != null ?
                    remotePeerId : (transfer != null ?
                    transfer.getRemotePeerId() : null);
            this.recordType = recordType;
            this.dataInfo = dataInfo;
            this.transfer = transfer;
            this.cause = cause;
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public RecordType getRecordType() {
            return recordType;
        }

        public PeerId getLocalPeerId() {
            return localPeerId;
        }

        public PeerId getRemotePeerId() {
            return remotePeerId;
        }

        public Map<String, DataInfo> getDataInfo() {
            return dataInfo;
        }

        public Transfer getTransfer() {
            return transfer;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "timeStamp=" + timeStamp +
                    ", recordType=" + recordType +
                    ", localPeerId=" + localPeerId +
                    ", dataInfo=" + dataInfo +
                    ", transfer=" + transfer +
                    ", cause=" + cause +
                    '}';
        }

        @Override
        public int compareTo(Record o) {
            return getTimeStamp().compareTo(o.getTimeStamp());
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
        records.add(Record.announced(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void peerCollectedDataInfo(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        super.peerCollectedDataInfo(peer, remotePeerId, dataInfo);
        records.add(Record.collected(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void peerRejectedUpload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerRejectedUpload(peer, transferManager, cause);
        records.add(Record.uploadRejected(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void peerStartedUpload(Peer peer, TransferManager transferManager) {
        super.peerStartedUpload(peer, transferManager);
        records.add(Record.uploadStarted(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerSucceededUpload(Peer peer, TransferManager transferManager) {
        super.peerSucceededUpload(peer, transferManager);
        records.add(Record.uploadSucceeded(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerRequestedDownload(Peer peer, TransferManager transferManager) {
        super.peerRequestedDownload(peer, transferManager);
        records.add(Record.downloadRequested(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerRejectedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerRejectedDownload(peer, transferManager, cause);
        records.add(Record.downloadRejected(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void peerStartedDownload(Peer peer, TransferManager transferManager) {
        super.peerStartedDownload(peer, transferManager);
        records.add(Record.downloadStarted(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerProgressedDownload(Peer peer, TransferManager transferManager) {
        super.peerProgressedDownload(peer, transferManager);
        records.add(Record.downloadProgressed(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerSucceededDownload(Peer peer, TransferManager transferManager) {
        super.peerSucceededDownload(peer, transferManager);
        records.add(Record.downloadSucceeded(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void peerFailedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        super.peerFailedDownload(peer, transferManager, cause);
        records.add(Record.downloadFailed(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }
}
