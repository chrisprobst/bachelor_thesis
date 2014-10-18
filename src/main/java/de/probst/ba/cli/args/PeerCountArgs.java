package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class PeerCountArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(PeerCountArgs.class);

    // Queues for storing instances
    private final Queue<Peer> peerQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Seeder> seederQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Leecher> leecherQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> superSeederQueue = new ConcurrentLinkedQueue<>();

    @Parameter(names = {"-ss", "--super-seeders"},
               description = "Number of super seeders (" + Validators.PeerCountValidator.MSG + ")",
               validateValueWith = Validators.PeerCountValidator.class,
               required = true)
    public Integer superSeeders;

    @Parameter(names = {"-sl", "--seeder-leecher-couples"},
               description = "Number of seeder-leecher couples (" + Validators.PeerCountValidator.MSG + ")",
               validateValueWith = Validators.PeerCountValidator.class,
               required = true)
    public Integer seederLeecherCouples;

    public Queue<Peer> getPeerQueue() {
        return peerQueue;
    }

    public Queue<Seeder> getSeederQueue() {
        return seederQueue;
    }

    public Queue<Leecher> getLeecherQueue() {
        return leecherQueue;
    }

    public Queue<Peer> getSuperSeederQueue() {
        return superSeederQueue;
    }

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ PeerCount Config ]");
        logger.info(">>> SuperSeeders:              " + superSeeders);
        logger.info(">>> Seeder-Leecher couples:    " + seederLeecherCouples);
        return true;
    }
}
