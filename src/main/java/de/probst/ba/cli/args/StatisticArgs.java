package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class StatisticArgs implements Args {

    @Parameter(names = {"-re", "--record-events"},
               description = "Record the events and serialize them")
    public Boolean recordEvents = false;

    @Parameter(names = {"-rs", "--record-statistics"},
               description = "Record statistics and save them in cvs form")
    public Boolean recordStatistics = false;

    @Parameter(names = {"-rd", "--records-directory"},
               description = "The directory to save the records",
               converter = FileConverter.class)
    public File recordsDirectory = new File(".");

    @Override
    public boolean check(JCommander jCommander) {
        recordsDirectory = recordsDirectory.toPath().toAbsolutePath().normalize().toFile();
        if (!recordsDirectory.mkdirs() && !recordsDirectory.exists()) {
            System.out.println("The records directory could not be created");
            return false;
        }

        if (!recordsDirectory.isDirectory()) {
            System.out.println("The records directory is not a directory");
            return false;
        }

        return true;
    }
}
