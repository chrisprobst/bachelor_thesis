package de.probst.ba.core.logic.brains;

import de.probst.ba.core.logic.Brain;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Brains {

    private Brains() {
    }

    public static Brain logarithmicBrain() {
        return new LogarithmicOrderedBrain();
    }

    public static Brain intelligentBrain() {
        return new IntelligentOrderedBrain();
    }
}
