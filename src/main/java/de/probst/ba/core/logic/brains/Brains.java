package de.probst.ba.core.logic.brains;

import de.probst.ba.core.logic.Brain;

/**
 * Created by chrisprobst on 17.08.14.
 */
public class Brains {

    private Brains() {
    }

    public static Brain logarithmicBrain() {
        return new DefaultTotalOrderedBrain();
    }

    public static Brain intelligentBrain() {
        return new IntelligentOrderedBrain();
    }
}
