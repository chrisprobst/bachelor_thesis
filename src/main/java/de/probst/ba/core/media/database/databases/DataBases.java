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
        return fileDataBase(false, directory);
    }

    public static DataBase fileDataBase(boolean allowOverwrite, Path directory) throws IOException {
        return new FileDataBase(allowOverwrite, directory);
    }

    public static DataBase memoryDataBase() {
        return memoryDataBase(false);
    }

    public static DataBase memoryDataBase(boolean allowOverwrite) {
        return new MemoryDataBase(allowOverwrite);
    }


    public static DataBase fakeDataBase(boolean allowOverwrite) {
        return new FakeDataBase(allowOverwrite);
    }

    public static DataBase fakeDataBase() {
        return fakeDataBase(false);
    }
}
