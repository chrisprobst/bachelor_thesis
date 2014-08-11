package de.probst.ba.test;
import static org.junit.Assert.*;

import de.probst.ba.core.logic.DataInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class DataInfoTest {

    private DataInfo dataInfo;

    @Before
    public void setUp() {
        dataInfo = new DataInfo("123Hash123", 100, 11);
    }

    @Test public void flip()
    {
        dataInfo = dataInfo.randomize();
        DataInfo flip = dataInfo.flip();

        assertEquals(dataInfo.getSize(), flip.getSize());
        assertEquals(dataInfo.getChunkCount(), flip.getChunkCount());
        assertEquals(dataInfo.getHash(), flip.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(dataInfo.isChunkCompleted(i), !flip.isChunkCompleted(i));
        }
    }

    @Test public void duplicate()
    {
        dataInfo = dataInfo.randomize();
        DataInfo duplicate = dataInfo.duplicate();

        assertEquals(dataInfo.getSize(), duplicate.getSize());
        assertEquals(dataInfo.getChunkCount(), duplicate.getChunkCount());
        assertEquals(dataInfo.getHash(), duplicate.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(dataInfo.isChunkCompleted(i), duplicate.isChunkCompleted(i));
        }
    }

    @Test public void contain()
    {
        // empty set does not contain empty set
        DataInfo duplicate = dataInfo.duplicate();
        assertFalse(dataInfo.contains(duplicate));

        // Same number of chunks
        dataInfo = dataInfo.setChunk(4, true);
        duplicate = dataInfo.duplicate();
        assertTrue(dataInfo.contains(duplicate));

        // Less chunks
        dataInfo = dataInfo.setChunk(2, true);
        dataInfo = dataInfo.setChunk(3, true);

        duplicate = dataInfo.duplicate();
        duplicate = duplicate.setChunk(3, false);
        duplicate = duplicate.setChunk(4, false);

        assertTrue(dataInfo.contains(duplicate));

        // More chunks
        duplicate = duplicate.setChunk(5, true);
        duplicate = duplicate.setChunk(6, true);

        assertFalse(dataInfo.contains(duplicate));

        duplicate = duplicate.setChunk(3, true);
        duplicate = duplicate.setChunk(4, true);
        assertTrue(duplicate.contains(dataInfo));
    }

    @Test public void full()
    {
        dataInfo = dataInfo.randomize();
        DataInfo full = dataInfo.full();

        assertEquals(dataInfo.getSize(), full.getSize());
        assertEquals(dataInfo.getChunkCount(), full.getChunkCount());
        assertEquals(dataInfo.getHash(), full.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(true, full.isChunkCompleted(i));
        }
    }

    @Test public void empty()
    {
        dataInfo = dataInfo.randomize();
        DataInfo empty = dataInfo.empty();

        assertEquals(dataInfo.getSize(), empty.getSize());
        assertEquals(dataInfo.getChunkCount(), empty.getChunkCount());
        assertEquals(dataInfo.getHash(), empty.getHash());

        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            assertEquals(false, empty.isChunkCompleted(i));
        }
    }

    @Test public void chunkIndex()
    {
        assertEquals(9, dataInfo.getChunkSize(0));
        assertEquals(9, dataInfo.getChunkSize(4));
        assertEquals(9, dataInfo.getChunkSize(9));
        assertEquals(1, dataInfo.getChunkSize(10));
    }

    @Test public void chunk()
    {

        dataInfo = dataInfo.setChunk(3, true);
        dataInfo = dataInfo.setChunk(7, true);

        assertTrue(dataInfo.isChunkCompleted(3));
        assertTrue(dataInfo.isChunkCompleted(7));

        assertFalse(dataInfo.isChunkCompleted(1));
        assertFalse(dataInfo.isChunkCompleted(4));
        assertFalse(dataInfo.isChunkCompleted(10));
    }
}
