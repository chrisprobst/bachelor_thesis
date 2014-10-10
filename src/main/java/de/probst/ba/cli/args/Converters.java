package de.probst.ba.cli.args;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.net.peer.peers.Peers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class Converters {

    private Converters() {
    }

    public static class PeerTypeConverter extends BaseConverter<Peers.PeerType> {

        public PeerTypeConverter(String optionName) {
            super(optionName);
        }

        @Override
        public Peers.PeerType convert(String value) {
            try {
                return Peers.PeerType.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException(getErrorString(value, Arrays.toString(Peers.PeerType.values())));
            }
        }
    }

    public static class AlgorithmTypeConverter extends BaseConverter<Algorithms.AlgorithmType> {

        public AlgorithmTypeConverter(String optionName) {
            super(optionName);
        }

        @Override
        public Algorithms.AlgorithmType convert(String value) {
            try {
                return Algorithms.AlgorithmType.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException(getErrorString(value, Arrays.toString(Algorithms.AlgorithmType.values())));
            }
        }
    }

    public static class HostNameConverter extends BaseConverter<InetAddress> {

        public HostNameConverter(String optionName) {
            super(optionName);
        }

        @Override
        public InetAddress convert(String value) {
            try {
                return InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                throw new ParameterException(getErrorString(value, "a host name, cause " + e.getMessage()));
            }
        }
    }
}
