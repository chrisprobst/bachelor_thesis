package de.probst.ba.test;
import static org.junit.Assert.*;

import de.probst.ba.core.Data;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class DataTest {

    private Data data;

    @Before
    public void setUp() {
        data = new Data("123Hash123", 100, 11);
    }

    @Test public void flip()
    {
        Data empty = data.flip();

        assertEquals(data.getSize(), empty.getSize());
        assertEquals(data.getChunkCount(), empty.getChunkCount());
        assertEquals(data.getHash(), empty.getHash());

        for (int i = 0; i < data.getChunkCount(); i++) {
            assertEquals(data.isChunkCompleted(i), !empty.isChunkCompleted(i));
        }
    }

    @Test public void duplicate()
    {
        Data empty = data.duplicate();

        assertEquals(data.getSize(), empty.getSize());
        assertEquals(data.getChunkCount(), empty.getChunkCount());
        assertEquals(data.getHash(), empty.getHash());

        for (int i = 0; i < data.getChunkCount(); i++) {
            assertEquals(data.isChunkCompleted(i), empty.isChunkCompleted(i));
        }
    }

    @Test public void empty()
    {
        Data empty = data.empty();

        assertEquals(data.getSize(), empty.getSize());
        assertEquals(data.getChunkCount(), empty.getChunkCount());
        assertEquals(data.getHash(), empty.getHash());

        for (int i = 0; i < data.getChunkCount(); i++) {
            assertEquals(false, empty.isChunkCompleted(i));
        }
    }

    @Test public void chunkIndex()
    {
        assertEquals(9, data.getChunkSize(0));
        assertEquals(9, data.getChunkSize(4));
        assertEquals(9, data.getChunkSize(9));
        assertEquals(1, data.getChunkSize(10));
    }

    @Test public void chunk()
    {
        data.setChunk(3, true);
        data.setChunk(7, true);

        assertTrue(data.isChunkCompleted(3));
        assertTrue(data.isChunkCompleted(7));

        assertFalse(data.isChunkCompleted(1));
        assertFalse(data.isChunkCompleted(4));
        assertFalse(data.isChunkCompleted(10));
    }
}
