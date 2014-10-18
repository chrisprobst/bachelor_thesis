package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class DataInfoGeneratorArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(DataInfoGeneratorArgs.class);
    private final Queue<Peer> dataBaseUpdatePeers = new ConcurrentLinkedQueue<>();

    @Parameter(names = {"-p", "--partitions"},
               description = "The number of partitions (" + Validators.PartitionValidator.MSG + ")",
               validateValueWith = Validators.PartitionValidator.class)
    public Integer partitions = 1;

    @Parameter(names = {"-cc", "--chunk-count"},
               description = "The number of chunks (" + Validators.ChunkCountValidator.MSG + ")",
               validateValueWith = Validators.ChunkCountValidator.class)
    public Integer chunkCount = 1;

    @Parameter(names = {"-s", "--size"},
               description = "The size in bytes (" + Validators.SizeValidator.MSG + ")",
               validateValueWith = Validators.SizeValidator.class,
               required = true)
    public Long size;

    public long partitionSize;

    public long chunkSize;

    private DataInfo generateSingleDataInfo(int partition, long partitionSize) {
        return DataInfo.generate(partition,
                                 partitionSize,
                                 Optional.empty(),
                                 Optional.empty(),
                                 "Generated hash for partition " + partition,
                                 chunkCount,
                                 String::valueOf).full();
    }

    public List<Tuple2<DataInfo, Supplier<ReadableByteChannel>>> generateDataInfo() {
        long lastPartitionSize = size - partitionSize * (partitions - 1);

        Function<DataInfo, Supplier<ReadableByteChannel>> channelGenerator =
                dataInfo -> () -> Channels.newChannel(new ByteArrayInputStream(new byte[(int) dataInfo.getSize()]));

        return IntStream.range(0, partitions)
                        .mapToObj(partition -> generateSingleDataInfo(partition,
                                                                      partition < partitions - 1 ?
                                                                      partitionSize :
                                                                      lastPartitionSize))
                        .map(dataInfo -> Tuple.of(dataInfo, channelGenerator.apply(dataInfo)))
                        .collect(Collectors.toList());
    }

    public Queue<Peer> getDataBaseUpdatePeers() {
        return dataBaseUpdatePeers;
    }

    public boolean check(JCommander jCommander) {
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

        logger.info(">>> [ DataInfo Generator Config ]");
        logger.info(">>> Size:              " + size);
        logger.info(">>> Partitions:        " + partitions);
        logger.info(">>> Partition size:    " + partitionSize);
        logger.info(">>> Chunk count:       " + chunkCount);
        logger.info(">>> Chunk size:        " + chunkSize);

        return true;
    }
}
