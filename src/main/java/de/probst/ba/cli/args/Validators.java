package de.probst.ba.cli.args;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class Validators {

    private Validators() {
    }

    public static class PortValidator implements IValueValidator<Integer> {

        public static final int MIN = 0;
        public static final int MAX = 65535;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class MaxConnectionsValidator implements IValueValidator<Integer> {

        public static final int MIN = 0;
        public static final int MAX = Integer.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PeerCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = Integer.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class TransferRateValidator implements IValueValidator<Integer> {

        public static final int MIN = 0;
        public static final int MAX = Integer.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class SizeValidator implements IValueValidator<Long> {

        public static final long MIN = 1;
        public static final long MAX = Long.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Long value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class ChunkCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = Integer.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PartitionValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = Integer.MAX_VALUE;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PercentageValidator implements IValueValidator<Double> {

        public static final double MIN = 0;
        public static final double MAX = 100;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Double value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }
}
