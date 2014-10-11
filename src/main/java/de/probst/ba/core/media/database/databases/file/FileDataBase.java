package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;
import de.probst.ba.core.util.io.IOUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 09.10.14.
 */
public final class FileDataBase extends AbstractDataBase {

    public static final String LOCK_FILE_NAME = "db.lock";

    private final Path directory;
    private final FileChannel lockFile;
    private final Map<DataInfo, FileChannel> fileChannels = new HashMap<>();

    @Override
    protected void doClose() throws IOException {
        try {
            IOUtil.closeAllAndThrow(fileChannels.values());
        } finally {
            fileChannels.clear();
            lockFile.close();
        }
    }

    public FileDataBase(Path directory) throws IOException {
        Objects.requireNonNull(directory);

        // Make sure the directory exists
        Files.createDirectories(directory);

        // Open lock file
        lockFile = FileChannel.open(directory.resolve(LOCK_FILE_NAME),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.SYNC);

        // Try to lock the file
        FileLock fileLock = lockFile.tryLock();
        if (fileLock == null || !fileLock.isValid()) {
            try {
                throw new IOException("fileLock == null || !fileLock.isValid()");
            } finally {
                lockFile.close();
            }
        }

        // Save the database directory
        this.directory = directory;
    }

    @Override
    protected AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        DataInfo full = writeDataInfo.full();
        FileChannel fileChannel = fileChannels.get(full);
        if (fileChannel == null) {
            fileChannels.put(full, fileChannel = FileChannel.open(directory.resolve(full.getHash()),
                                                                  StandardOpenOption.CREATE,
                                                                  StandardOpenOption.WRITE,
                                                                  StandardOpenOption.READ));
        }
        return new FileDataBaseWriteChannel(this, writeDataInfo, fileChannel);
    }

    @Override
    protected AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new FileDataBaseReadChannel(this, readDataInfo, fileChannels.get(readDataInfo.full()));
    }
}
