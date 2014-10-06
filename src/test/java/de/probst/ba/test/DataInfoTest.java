package de.probst.ba.test;

import de.probst.ba.core.media.database.DataInfo;
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
        assertEquals(5 * 9, dataInfo.getOffset(5));
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
}
