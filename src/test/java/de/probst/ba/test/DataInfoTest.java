package de.probst.ba.test;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.DataInfoRegionLock;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class DataInfoTest {

    private DataInfo dataInfo;

    @Before
    public void setUp() {

        // 11 parts are needed for this tests
        dataInfo = DataInfo.generate(0,
                                     100,
                                     Optional.empty(),
                                     Optional.empty(),
                                     "123Hash123",
                                     11,
                                     String::valueOf);
    }

    @Test
    public void randomChunk() {
        DataInfo a = dataInfo.randomize();
        DataInfo b = a.withOneCompletedChunk();
        assertTrue(a.contains(b));
        assertTrue(b.getCompletedChunkCount() == 1);
    }

    @Test
    public void union() {
        DataInfo a = dataInfo
                .withChunk(2)
                .withChunk(4);

        DataInfo b = dataInfo
                .withChunk(4)
                .withChunk(5)
                .withChunk(6);

        DataInfo c = dataInfo
                .withChunk(2)
                .withChunk(4)
                .withChunk(5)
                .withChunk(6);

        assertEquals(a.union(b), c);
    }

    @Test
    public void offset() {
        DataInfo a = dataInfo.withChunk(4).withChunk(5).withChunk(6);
        DataInfo c = dataInfo.withChunk(2).withChunk(5).withChunk(10);

        assertEquals(0, dataInfo.getTotalOffset(0));
        assertEquals(9, dataInfo.getTotalOffset(1));
        assertEquals(90, dataInfo.getTotalOffset(10));
        assertEquals(5 * 9, dataInfo.getTotalOffset(5));

        assertEquals(0, a.getRelativeOffset(4));
        assertEquals(9, a.getRelativeOffset(5));
        assertEquals(18, a.getRelativeOffset(6));

        assertEquals(0, dataInfo.getTotalChunkIndex(4, true));
        assertEquals(0, dataInfo.getTotalChunkIndex(8, true));
        assertEquals(1, dataInfo.getTotalChunkIndex(9, true));
        assertEquals(5, dataInfo.getTotalChunkIndex(5 * 9, true));
        assertEquals(4, dataInfo.getTotalChunkIndex(5 * 9 - 1, true));
        assertEquals(3, dataInfo.getTotalChunkIndex(3 * 9, true));
        assertEquals(2, dataInfo.getTotalChunkIndex(3 * 9 - 1, true));

        assertEquals(a.getTotalChunkIndex(4 * 9, true), a.getTotalChunkIndex(0, false));
        assertEquals(a.getTotalChunkIndex(5 * 9, true), a.getTotalChunkIndex(9, false));
        assertEquals(a.getTotalChunkIndex(6 * 9, true), a.getTotalChunkIndex(2 * 9, false));

        assertEquals(c.getTotalChunkIndex(2 * 9, true), c.getTotalChunkIndex(0, false));
        assertEquals(c.getTotalChunkIndex(5 * 9, true), c.getTotalChunkIndex(9, false));
        assertEquals(c.getTotalChunkIndex(10 * 9, true), c.getTotalChunkIndex(2 * 9, false));
    }

    @Test
    public void substract() {
        DataInfo a = dataInfo
                .withChunk(2)
                .withChunk(4)
                .withChunk(5);

        DataInfo b = dataInfo
                .withChunk(4)
                .withChunk(5)
                .withChunk(6);

        DataInfo c = dataInfo
                .withChunk(2);

        assertEquals(a.subtract(b), c);
    }

    @Test
    public void intersection() {
        DataInfo a = dataInfo
                .withChunk(2)
                .withChunk(4)
                .withChunk(5);

        DataInfo b = dataInfo
                .withChunk(4)
                .withChunk(5)
                .withChunk(6);

        DataInfo c = dataInfo
                .withChunk(4)
                .withChunk(5);

        assertEquals(a.intersection(b), c);
    }

    @Test
    public void whereWithWithout() {
        DataInfo a = dataInfo
                .withChunk(2)
                .withChunk(4)
                .whereChunk(5, true)
                .whereChunk(6, false)
                .withoutChunk(4);

        DataInfo b = dataInfo
                .withChunk(6)
                .whereChunk(2, true)
                .withChunk(4)
                .whereChunk(4, false)
                .withChunk(5)
                .withoutChunk(6)
                .withoutChunk(4);

        assertEquals(a, b);
    }

    @Test
    public void flip() {
        dataInfo = dataInfo.randomize();
        DataInfo flip = dataInfo.flip();

        assertEquals(dataInfo.getSize(), flip.getSize());
        assertEquals(dataInfo.getChunkHashes(), flip.getChunkHashes());
        assertEquals(dataInfo.getHash(), flip.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(dataInfo.isChunkCompleted(i), !flip.isChunkCompleted(i));
        }
    }

    @Test
    public void duplicate() {
        dataInfo = dataInfo.randomize();
        DataInfo duplicate = dataInfo.duplicate();

        assertEquals(dataInfo.getSize(), duplicate.getSize());
        assertEquals(dataInfo.getChunkHashes(), duplicate.getChunkHashes());
        assertEquals(dataInfo.getHash(), duplicate.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(dataInfo.isChunkCompleted(i), duplicate.isChunkCompleted(i));
        }

        assertEquals(duplicate, dataInfo);
    }

    @Test
    public void contain() {
        // Empty set contains empty set
        DataInfo duplicate = dataInfo.duplicate();
        assertTrue(dataInfo.contains(duplicate));

        // Same number of chunks
        dataInfo = dataInfo.whereChunk(4, true);
        duplicate = dataInfo.duplicate();
        assertTrue(dataInfo.contains(duplicate));

        // Less chunks
        dataInfo = dataInfo.whereChunk(2, true);
        dataInfo = dataInfo.whereChunk(3, true);

        duplicate = dataInfo.duplicate();
        duplicate = duplicate.whereChunk(3, false);
        duplicate = duplicate.whereChunk(4, false);

        assertTrue(dataInfo.contains(duplicate));

        // More chunks
        duplicate = duplicate.whereChunk(5, true);
        duplicate = duplicate.whereChunk(6, true);

        assertFalse(dataInfo.contains(duplicate));

        duplicate = duplicate.whereChunk(3, true);
        duplicate = duplicate.whereChunk(4, true);
        assertTrue(duplicate.contains(dataInfo));
    }

    @Test
    public void overlaps() {
        // Empty set does not overlap with empty set
        DataInfo duplicate = dataInfo.duplicate();
        assertFalse(dataInfo.overlaps(duplicate));

        // Same number of chunks
        dataInfo = dataInfo.whereChunk(4, true);
        duplicate = dataInfo.duplicate();
        assertTrue(dataInfo.overlaps(duplicate));

        // Less chunks
        dataInfo = dataInfo.whereChunk(2, true);
        dataInfo = dataInfo.whereChunk(3, true);

        duplicate = dataInfo.duplicate();
        duplicate = duplicate.whereChunk(3, false);
        duplicate = duplicate.whereChunk(4, false);

        assertTrue(dataInfo.overlaps(duplicate));

        // More chunks
        duplicate = duplicate.whereChunk(5, true);
        duplicate = duplicate.whereChunk(6, true);

        assertTrue(dataInfo.overlaps(duplicate));

        duplicate = duplicate.empty().withChunk(8).withChunk(9);
        assertFalse(duplicate.overlaps(dataInfo));
    }

    @Test
    public void full() {
        dataInfo = dataInfo.randomize();
        DataInfo full = dataInfo.full();

        assertEquals(dataInfo.getSize(), full.getSize());
        assertEquals(dataInfo.getChunkHashes(), full.getChunkHashes());
        assertEquals(dataInfo.getHash(), full.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(true, full.isChunkCompleted(i));
        }
    }

    @Test
    public void empty() {
        dataInfo = dataInfo.randomize();
        DataInfo empty = dataInfo.empty();

        assertEquals(dataInfo.getSize(), empty.getSize());
        assertEquals(dataInfo.getChunkHashes(), empty.getChunkHashes());
        assertEquals(dataInfo.getHash(), empty.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(false, empty.isChunkCompleted(i));
        }
    }

    @Test
    public void chunkIndex() {
        assertEquals(9, dataInfo.getChunkSize(0));
        assertEquals(9, dataInfo.getChunkSize(4));
        assertEquals(9, dataInfo.getChunkSize(9));
        assertEquals(10, dataInfo.getChunkSize(10));
    }

    @Test
    public void chunk() {

        dataInfo = dataInfo.whereChunk(3, true);
        dataInfo = dataInfo.whereChunk(7, true);

        assertTrue(dataInfo.isChunkCompleted(3));
        assertTrue(dataInfo.isChunkCompleted(7));

        assertFalse(dataInfo.isChunkCompleted(1));
        assertFalse(dataInfo.isChunkCompleted(4));
        assertFalse(dataInfo.isChunkCompleted(10));
    }

    @Test
    public void calculateCompletedChunks() {
        assertEquals(0, dataInfo.calculateCompletedChunksForSize(50).count());
        dataInfo = dataInfo.withChunk(2).withChunk(4).withChunk(6);
        assertEquals(2, dataInfo.calculateCompletedChunksForSize(18).count());
        assertEquals(2, dataInfo.calculateCompletedChunksForSize(26).count());
        assertEquals(3, dataInfo.calculateCompletedChunksForSize(36).count());
    }

    @Test
    public void regionLock() {
        DataInfoRegionLock dataInfoRegionLock = new DataInfoRegionLock();
        DataInfo lockA = dataInfo.withChunk(3).withChunk(5);
        DataInfo lockB = dataInfo.withChunk(4).withChunk(6);
        DataInfo lockC = dataInfo.withChunk(2).withChunk(7);
        DataInfo lockD = dataInfo.full();

        dataInfoRegionLock.lockWriteResource(lockA);
        dataInfoRegionLock.lockWriteResource(lockB);
        dataInfoRegionLock.lockWriteResource(lockC);
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockA));
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockB));
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockC));
        dataInfoRegionLock.lockWriteResource(lockA);
        dataInfoRegionLock.lockWriteResource(lockB);
        dataInfoRegionLock.lockWriteResource(lockC);
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockA));
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockB));
        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockC));
        dataInfoRegionLock.lockWriteResource(lockA);

        dataInfoRegionLock.lockReadResource(lockB);
        dataInfoRegionLock.lockReadResource(lockB);
        dataInfoRegionLock.lockReadResource(lockC);
        dataInfoRegionLock.lockReadResource(lockC);

        assertFalse(dataInfoRegionLock.tryLockReadResource(lockD));

        assertTrue(dataInfoRegionLock.tryUnlockWriteResource(lockA));

        assertTrue(dataInfoRegionLock.tryLockReadResource(lockD));
        assertTrue(dataInfoRegionLock.tryUnlockReadResource(lockD));
    }
}
