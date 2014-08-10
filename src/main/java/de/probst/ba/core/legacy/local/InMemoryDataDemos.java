package de.probst.ba.core.legacy.local;

import de.probst.ba.core.logic.DataInfo;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryDataDemos {
    private InMemoryDataDemos() {
    }

    public static final DataInfo MOVIE_A = new DataInfo("Movie A", 1000 * 1000 * 2, 500);

    public static final DataInfo MOVIE_B = new DataInfo("Movie B", 1000 * 1000 * 2, 500);
}
