package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;

/**
 * Created by chrisprobst on 10.10.14.
 */
public interface Args {

    default boolean check(JCommander jCommander) {
        return true;
    }
}
