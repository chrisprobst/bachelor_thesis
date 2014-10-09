package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.databases.fake.FakeDataBase;
import de.probst.ba.core.media.database.databases.file.FileDataBase;
import de.probst.ba.core.media.database.databases.memory.MemoryDataBase;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class DataBases {

    private DataBases() {

    }

    public static DataBase fileDataBase(Path directory) throws IOException {
        return new FileDataBase(directory);
    }

    public static DataBase memoryDataBase() {
        return new MemoryDataBase();
    }

    public static DataBase fakeDataBase() {
        return new FakeDataBase();
    }
}
