package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface RecreationEndsListener extends RuinAndRecreateListener {

	public void informRecreationEnds(RuinAndRecreateSolution solution);

}
