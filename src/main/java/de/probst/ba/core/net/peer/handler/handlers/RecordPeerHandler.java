package de.probst.ba.core.net.peer.handler.handlers;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;

import java.io.Serializable;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class RecordPeerHandler implements LeecherPeerHandler, SeederPeerHandler {

    private final Queue<Record> records = new ConcurrentLinkedQueue<>();
    private volatile Record start;
    private volatile Record end;

    public List<Record> sortAndGetRecords() {
        Record start = getStart();
        Record end = getEnd();

        if (start != null && end != null &&
            end.getTimeStamp().compareTo(start.getTimeStamp()) < 0) {
            throw new IllegalStateException("End record before start record");
        }

        List<Record> results = records.stream()
                                      .filter(r -> start == null || r.getTimeStamp()
                                                                     .compareTo(start.getTimeStamp()) >= 0)
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

    @Override
    public void discoveredSocketAddress(Seeder seeder, SocketAddress remoteSocketAddress) {

    }

    @Override
    public void announced(Seeder seeder, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
        records.add(Record.announced(seeder.getPeerId(), remotePeerId, dataInfo));
    }

    @Override
    public void uploadRejected(Seeder seeder, Transfer transfer, Throwable cause) {
        records.add(Record.uploadRejected(seeder.getPeerId(), transfer, cause));
    }

    @Override
    public void uploadStarted(Seeder seeder, Transfer transfer) {
        records.add(Record.uploadStarted(seeder.getPeerId(), transfer));
    }

    @Override
    public void uploadSucceeded(Seeder seeder, Transfer transfer) {
        records.add(Record.uploadSucceeded(seeder.getPeerId(), transfer));
    }

    @Override
    public void discoveredSocketAddresses(Leecher leecher, Set<SocketAddress> socketAddresses) {

    }

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
        records.add(Record.collected(leecher.getPeerId(), remotePeerId, dataInfo));
    }

    @Override
    public void downloadRequested(Leecher leecher, Transfer transfer) {
        records.add(Record.downloadRequested(leecher.getPeerId(), transfer));
    }

    @Override
    public void downloadRejected(Leecher leecher, Transfer transfer, Throwable cause) {
        records.add(Record.downloadRejected(leecher.getPeerId(), transfer, cause));
    }

    @Override
    public void downloadStarted(Leecher leecher, Transfer transfer) {
        records.add(Record.downloadStarted(leecher.getPeerId(), transfer));
    }

    @Override
    public void downloadProgressed(Leecher leecher, Transfer transfer) {
        records.add(Record.downloadProgressed(leecher.getPeerId(), transfer));
    }

    @Override
    public void downloadSucceeded(Leecher leecher, Transfer transfer) {
        records.add(Record.downloadSucceeded(leecher.getPeerId(), transfer));
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, Transfer transfer) {
        records.add(Record.dataCompleted(leecher.getPeerId(), dataInfo, transfer));
    }

    public enum RecordType {
        Start, End,
        Announced, Collected,
        UploadStarted, UploadRejected, UploadSucceeded,
        DownloadRequested, DownloadRejected, DownloadStarted, DownloadProgressed, DownloadSucceeded,
        DataCompleted
    }

    public final static class Record implements Comparable<Record>, Serializable {

        private final Instant timeStamp = Instant.now();
        private final RecordType recordType;
        private final PeerId localPeerId;
        private final PeerId remotePeerId;
        private final Map<String, DataInfo> dataInfo;
        private final DataInfo completedDataInfo;
        private final Transfer transfer;
        private final Throwable cause;

        private Record(RecordType recordType,
                       PeerId localPeerId,
                       PeerId remotePeerId,
                       Map<String, DataInfo> dataInfo,
                       DataInfo completedDataInfo,
                       Transfer transfer,
                       Throwable cause) {
            Objects.requireNonNull(recordType);
            this.localPeerId = localPeerId;
            this.remotePeerId =
                    remotePeerId != null ? remotePeerId : (transfer != null ? transfer.getRemotePeerId() : null);
            this.recordType = recordType;
            this.dataInfo = dataInfo;
            this.completedDataInfo = completedDataInfo;
            this.transfer = transfer;
            this.cause = cause;
        }

        private static Record start() {
            return new Record(RecordType.Start, null, null, null, null, null, null);
        }

        private static Record end() {
            return new Record(RecordType.End, null, null, null, null, null, null);
        }

        public static Record collected(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Collected, peerId, remotePeerId, dataInfo, null, null, null);
        }

        public static Record downloadRequested(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadRequested, peerId, null, null, null, transfer, null);
        }

        public static Record downloadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.DownloadRejected, peerId, null, null, null, transfer, cause);
        }

        public static Record downloadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadStarted, peerId, null, null, null, transfer, null);
        }

        public static Record downloadProgressed(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadProgressed, peerId, null, null, null, transfer, null);
        }

        public static Record downloadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.DownloadSucceeded, peerId, null, null, null, transfer, null);
        }

        public static Record dataCompleted(PeerId peerId, DataInfo completedDataInfo, Transfer lastTransfer) {
            return new Record(RecordType.DataCompleted, peerId, null, null, completedDataInfo, lastTransfer, null);
        }

        public static Record announced(PeerId peerId, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
            return new Record(RecordType.Announced, peerId, remotePeerId, dataInfo, null, null, null);
        }

        public static Record uploadStarted(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadStarted, peerId, null, null, null, transfer, null);
        }

        public static Record uploadRejected(PeerId peerId, Transfer transfer, Throwable cause) {
            return new Record(RecordType.UploadRejected, peerId, null, null, null, transfer, cause);
        }

        public static Record uploadSucceeded(PeerId peerId, Transfer transfer) {
            return new Record(RecordType.UploadSucceeded, peerId, null, null, null, transfer, null);
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public RecordType getRecordType() {
            return recordType;
        }

        public PeerId getPeerId() {
            return localPeerId;
        }

        public PeerId getRemotePeerId() {
            return remotePeerId;
        }

        public Map<String, DataInfo> getDataInfo() {
            return dataInfo;
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
}
