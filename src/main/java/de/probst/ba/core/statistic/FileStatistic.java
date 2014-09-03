package de.probst.ba.core.statistic;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by chrisprobst on 04.09.14.
 */
public abstract class FileStatistic extends Statistic implements Closeable {

    private final Path csvPath;

    public FileStatistic(Path csvPath) {
        Objects.requireNonNull(csvPath);
        this.csvPath = csvPath;
    }

    public synchronized void save() throws IOException {
        Files.write(csvPath, toString().getBytes());
    }

    @Override
    public synchronized void close() throws IOException {
        save();
    }
}
