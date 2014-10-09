package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;
import de.probst.ba.core.util.io.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
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
        return new FileDataBaseWriteChannel(writeDataInfo);
    }

    @Override
    protected AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new FileDataBaseReadChannel(readDataInfo);
    }

    private final class FileDataBaseReadChannel extends AbstractDataBaseReadChannel {

        public FileDataBaseReadChannel(DataInfo dataInfo) {
            super(FileDataBase.this, dataInfo);
        }

        @Override
        protected int doRead(ByteBuffer dst,
                             int chunkIndex,
                             long totalChunkOffset,
                             long relativeChunkOffset,
                             long chunkSize) throws IOException {

            // Cannot be null
            return fileChannels.get(getDataInfo().full())
                               .read(dst, totalChunkOffset + relativeChunkOffset);
        }

        @Override
        protected void doClose() throws IOException {

        }
    }

    private final class FileDataBaseWriteChannel extends AbstractDataBaseWriteChannel {

        private FileChannel fileChannel;

        private FileDataBaseWriteChannel(DataInfo dataInfo) {
            super(FileDataBase.this, dataInfo);
        }

        @Override
        protected int doWrite(ByteBuffer src,
                              int chunkIndex,
                              long totalChunkOffset,
                              long relativeChunkOffset,
                              long chunkSize) throws IOException {

            if (fileChannel == null) {
                // Get full representation
                DataInfo full = getDataInfo().full();

                // Lookup file channel
                if ((fileChannel = fileChannels.get(full)) == null) {
                    // Open a new file channel
                    fileChannel = FileChannel.open(directory.resolve(full.getHash()),
                                                   StandardOpenOption.CREATE,
                                                   StandardOpenOption.WRITE,
                                                   StandardOpenOption.READ);

                    // Insert in map
                    fileChannels.put(full, fileChannel);
                }
            }

            // Simply write the buffer at the specific place
            return fileChannel.write(src, totalChunkOffset + relativeChunkOffset);
        }

        @Override
        protected void doClose() throws IOException {
            if (fileChannel != null) {
                fileChannel.force(true);
            }
        }
    }
}
