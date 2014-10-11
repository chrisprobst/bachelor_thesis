package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.databases.DataBases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class FileDataBaseArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(FileDataBaseArgs.class);

    @Parameter(names = {"-dd", "--database-directory"},
               description = "The directory to store database records",
               converter = FileConverter.class)
    public File dataBaseDirectory = new File("./db");

    public DataBase getDataBase() throws IOException {
        return DataBases.fileDataBase(dataBaseDirectory.toPath());
    }

    @Override
    public boolean check(JCommander jCommander) {
        dataBaseDirectory = dataBaseDirectory.toPath().toAbsolutePath().normalize().toFile();
        if (!dataBaseDirectory.mkdirs() && !dataBaseDirectory.exists()) {
            System.out.println("The database directory could not be created");
            return false;
        }

        if (!dataBaseDirectory.isDirectory()) {
            System.out.println("The database directory is not a directory");
            return false;
        }

        logger.info(">>> [ File DataBase Config ]");
        logger.info(">>> DataBase directory:    " + dataBaseDirectory);

        return true;
    }
}
