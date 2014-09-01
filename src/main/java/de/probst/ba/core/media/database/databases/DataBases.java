package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.fake.FakeDataBase;
import de.probst.ba.core.media.database.databases.file.SingleFileDataBase;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class DataBases {

    private DataBases() {

    }

    public static DataBase singleFileDataBase(Path path, DataInfo singleDataInfo) throws IOException {
        return new SingleFileDataBase(path, singleDataInfo);
    }

    public static DataBase fakeDataBase(DataInfo... dataInfo) {
        return new FakeDataBase(dataInfo);
    }
}
