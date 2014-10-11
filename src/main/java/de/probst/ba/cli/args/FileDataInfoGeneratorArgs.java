package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.media.database.DataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class FileDataInfoGeneratorArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(FileDataInfoGeneratorArgs.class);

    @Parameter(names = {"-p", "--partitions"},
               description = "The number of partitions (" + Validators.PartitionValidator.MSG + ")",
               validateValueWith = Validators.PartitionValidator.class)
    public Integer partitions = 1;

    @Parameter(names = {"-cc", "--chunk-count"},
               description = "The number of chunks (" + Validators.ChunkCountValidator.MSG + ")",
               validateValueWith = Validators.ChunkCountValidator.class)
    public Integer chunkCount = 1;

    @Parameter(names = {"-df", "--data-file"},
               description = "The data file to generate the data info from",
               converter = FileConverter.class,
               required = true)
    public File dataFile;

    public long size;

    public long partitionSize;

    public long chunkSize;

    public FileChannel openFileChannel() throws IOException {
        return FileChannel.open(dataFile.toPath(), StandardOpenOption.READ);
    }

    public List<DataInfo> generateDataInfo() throws IOException, NoSuchAlgorithmException {
        try (FileChannel fileChannel = openFileChannel()) {

            // Create the description object
            Map<String, Object> descriptionObject = new HashMap<>();
            descriptionObject.put("total-size", fileChannel.size());
            descriptionObject.put("partitions", partitions);

            return DataInfo.fromPartitionedChannel(partitions,
                                                   size,
                                                   Optional.of(dataFile.getName()),
                                                   Optional.of(descriptionObject),
                                                   chunkCount,
                                                   fileChannel);
        }
    }

    @Override
    public boolean check(JCommander jCommander) {
        dataFile = dataFile.toPath().toAbsolutePath().normalize().toFile();
        if (!dataFile.exists()) {
            System.out.println("The data file does not exist");
            return false;
        }

        if (!dataFile.isFile()) {
            System.out.println("The data file is not a file");
            return false;
        }

        size = dataFile.length();
        if (partitions > size) {
            System.out.println("Invalid: partitions (" + partitions + ") > size (" + size + ")");
            return false;
        }

        partitionSize = size / partitions;
        if (chunkCount > partitionSize) {
            System.out.println("Invalid: chunkCount (" + chunkCount + ") > partitionSize (" + partitionSize + ")");
            return false;
        }
        chunkSize = partitionSize / chunkCount;

        logger.info(">>> [ File DataInfo Generator Config ]");
        logger.info(">>> Data file:         " + dataFile);
        logger.info(">>> Size:              " + size);
        logger.info(">>> Partitions:        " + partitions);
        logger.info(">>> Partition size:    " + partitionSize);
        logger.info(">>> Chunk count:       " + chunkCount);
        logger.info(">>> Chunk size:        " + chunkSize);

        return true;
    }
}
