package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.fake.FakeDataBase;
import de.probst.ba.core.media.databases.file.SingleFileDataBase;

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
