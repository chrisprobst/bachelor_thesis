package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class HelpArgs implements Args {

    @Parameter(names = {"-h", "--help"},
               description = "Show usage")
    public Boolean showUsage = false;

    @Override
    public boolean check(JCommander jCommander) {
        if (showUsage) {
            jCommander.usage();
            return false;
        }
        return true;
    }
}
