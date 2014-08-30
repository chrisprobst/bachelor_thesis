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
public class RecordDiagnostic implements Diagnostic {

    public enum RecordType {
        Start, End,
        Announced, Collected, InterestAdded,
        UploadStarted, UploadRejected, UploadSucceeded,
        DownloadRequested, DownloadRejected, DownloadStarted, DownloadProgressed, DownloadSucceeded,
        DataCompleted
    }

    public final static class Record implements Comparable<Record>, Serializable {

        private static Record start() {
            return new Record(RecordType.Start, null, null, null, null, null, null, null);
        }

        private static Record end() {
            return new Record(RecordType.End, null, null, null, null, null, null, null);
        }

        // DATA INFO

        public static Record announced(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Announced, peerId, remotePeerId, dataInfo, null, null, null, null);
        }

        public static Record collected(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Collected, peerId, remotePeerId, dataInfo, null, null, null, null);
        }

        public static Record interestAdded(PeerId peerId, PeerId remotePeerId, DataInfo addedDataInfo) {
            return new Record(RecordType.InterestAdded, peerId, remotePeerId, null, addedDataInfo, null, null, null);
        }


        // UPLOAD

        public static Record uploadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadStarted, peerId, null, null, null, null, transfer, null);
        }

        public static Record uploadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.UploadRejected, peerId, null, null, null, null, transfer, cause);
        }

        public static Record uploadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadSucceeded, peerId, null, null, null, null, transfer, null);
        }

        // DOWNLOAD

        public static Record downloadRequested(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadRequested, peerId, null, null, null, null, transfer, null);
        }

        public static Record downloadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.DownloadRejected, peerId, null, null, null, null, transfer, cause);
        }

        public static Record downloadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadStarted, peerId, null, null, null, null, transfer, null);
        }

        public static Record downloadProgressed(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadProgressed, peerId, null, null, null, null, transfer, null);
        }

        public static Record downloadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadSucceeded, peerId, null, null, null, null, transfer, null);
        }

        public static Record dataCompleted(PeerId peerId, DataInfo completedDataInfo, Transfer lastTransfer) {
            return new Record(RecordType.DataCompleted, peerId, null, null, null, completedDataInfo, lastTransfer, null);
        }

        private final Instant timeStamp = Instant.now();
        private final RecordType recordType;
        private final PeerId localPeerId;
        private final PeerId remotePeerId;
        private final Map<String, DataInfo> dataInfo;
        private final DataInfo addedDataInfo;
        private final DataInfo completedDataInfo;
        private final Transfer transfer;
        private final Throwable cause;

        private Record(RecordType recordType,
                       PeerId localPeerId,
                       PeerId remotePeerId,
                       Map<String, DataInfo> dataInfo,
                       DataInfo addedDataInfo,
                       DataInfo completedDataInfo,
                       Transfer transfer,
                       Throwable cause) {
            Objects.requireNonNull(recordType);
            this.localPeerId = localPeerId;
            this.remotePeerId = remotePeerId != null ?
                    remotePeerId : (transfer != null ?
                    transfer.getRemotePeerId() : null);
            this.recordType = recordType;
            this.dataInfo = dataInfo;
            this.addedDataInfo = addedDataInfo;
            this.completedDataInfo = completedDataInfo;
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

        public DataInfo getAddedDataInfo() {
            return addedDataInfo;
        }

        public DataInfo getCompletedDataInfo() {
            return completedDataInfo;
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
                    ", remotePeerId=" + remotePeerId +
                    ", dataInfo=" + dataInfo +
                    ", addedDataInfo=" + addedDataInfo +
                    ", completedDataInfo=" + completedDataInfo +
                    ", transfer=" + transfer +
                    ", cause=" + cause +
                    '}';
        }

        @Override
        public int compareTo(Record o) {
            return getTimeStamp().compareTo(o.getTimeStamp());
        }
    }

    private volatile Record start;
    private volatile Record end;
    private final Queue<Record> records = new ConcurrentLinkedQueue<>();

    public List<Record> sortAndGetRecords() {
        Record start = getStart();
        Record end = getEnd();

        if (start != null && end != null &&
                end.getTimeStamp().compareTo(start.getTimeStamp()) < 0) {
            throw new IllegalStateException("End record before start record");
        }

        List<Record> results = records.stream()
                .filter(r -> start == null || r.getTimeStamp().compareTo(start.getTimeStamp()) >= 0)
                .filter(r -> end == null || r.getTimeStamp().compareTo(end.getTimeStamp()) <= 0)
                .collect(Collectors.toList());

        if (start != null) {
            results.add(0, start);
        }
        if (end != null) {
            results.add(end);
        }

        return results;
    }

    public void start() {
        start = Record.start();
    }

    public Record getStart() {
        return start;
    }

    public void end() {
        end = Record.end();
    }

    public Record getEnd() {
        return end;
    }

    ////
    //// DIAGNOSTICS METHODS
    ////

    @Override
    public void announced(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        records.add(Record.announced(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void collected(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        records.add(Record.collected(peer.getNetworkState().getLocalPeerId(), remotePeerId, dataInfo.orElse(null)));
    }

    @Override
    public void interestAdded(Peer peer, PeerId remotePeerId, DataInfo addedDataInfo) {
        records.add(Record.interestAdded(peer.getNetworkState().getLocalPeerId(), remotePeerId, addedDataInfo));
    }

    @Override
    public void uploadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        records.add(Record.uploadRejected(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void uploadStarted(Peer peer, TransferManager transferManager) {
        records.add(Record.uploadStarted(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void uploadSucceeded(Peer peer, TransferManager transferManager) {
        records.add(Record.uploadSucceeded(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void downloadRequested(Peer peer, TransferManager transferManager) {
        records.add(Record.downloadRequested(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void downloadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        records.add(Record.downloadRejected(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer(), cause));
    }

    @Override
    public void downloadStarted(Peer peer, TransferManager transferManager) {
        records.add(Record.downloadStarted(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void downloadProgressed(Peer peer, TransferManager transferManager) {
        records.add(Record.downloadProgressed(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {
        records.add(Record.downloadSucceeded(peer.getNetworkState().getLocalPeerId(), transferManager.getTransfer()));
    }

    @Override
    public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
        records.add(Record.dataCompleted(peer.getNetworkState().getLocalPeerId(), dataInfo, lastTransferManager.getTransfer()));
    }
}
