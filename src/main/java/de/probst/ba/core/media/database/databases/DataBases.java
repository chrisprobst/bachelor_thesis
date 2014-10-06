package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.databases.fake.FakeDataBase;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class DataBases {

    private DataBases() {

    }

    public static DataBase fakeDataBase() {
        return new FakeDataBase();
    }
}
