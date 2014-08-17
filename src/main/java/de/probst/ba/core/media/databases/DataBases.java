package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;

/**
 * Created by chrisprobst on 17.08.14.
 */
public class DataBases {

    private DataBases() {

    }

    public static DataBase fakeDataBase(DataInfo... dataInfo) {
        return new FakeDataBase(dataInfo);
    }
}
