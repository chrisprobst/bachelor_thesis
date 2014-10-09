package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    public FileDataBase(Path directory) throws IOException {
        Objects.requireNonNull(directory);

        // Make sure the directory exists
        Files.createDirectories(directory);

        // Open lock file
        lockFile = FileChannel.open(directory.resolve(LOCK_FILE_NAME),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE);

        // Close lock file again
        if (lockFile.tryLock() == null) {
            try {
                throw new IOException("Could not lock database lock file");
            } finally {
                lockFile.close();
            }
        }

        // Save the database directory
        this.directory = directory;
    }

    @Override
    protected AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        return new AbstractDataBaseWriteChannel(this, writeDataInfo) {

            private FileChannel ref;

            @Override
            protected int doWrite(ByteBuffer src,
                                  int chunkIndex,
                                  long totalChunkOffset,
                                  long relativeChunkOffset,
                                  long chunkSize) throws IOException {
                // Get full representation
                DataInfo full = getDataInfo().full();

                // Make sure there is enough space
                FileChannel fileChannel = fileChannels.get(full);
                if (fileChannel == null) {
                    // Open a new file channel
                    ref = fileChannel = FileChannel.open(directory.resolve(full.getId() + "@" + full.getName().get()),
                                                         StandardOpenOption.CREATE,
                                                         StandardOpenOption.WRITE,
                                                         StandardOpenOption.READ);

                    // Insert in map
                    fileChannels.put(full, fileChannel);
                }

                // Simply write the buffer at the specific place
                return fileChannel.write(src, totalChunkOffset + relativeChunkOffset);
            }

            @Override
            protected void doClose() throws IOException {
                if (ref != null) {
                    ref.force(true);
                }
            }
        };
    }

    @Override
    protected AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new AbstractDataBaseReadChannel(this, readDataInfo) {

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
        };
    }

    @Override
    public synchronized void flush() throws IOException {
        for (FileChannel fileChannel : fileChannels.values()) {
            fileChannel.force(true);
        }
    }
}
